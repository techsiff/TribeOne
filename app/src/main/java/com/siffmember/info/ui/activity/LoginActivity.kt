@file:Suppress("DEPRECATION")

package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.firebase.Firebase
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityLoginBinding
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.Utils
import java.util.concurrent.TimeUnit
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.GoogleAuthProvider
import com.siffmember.info.R
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.ForgotPasswordRequest
import com.siffmember.info.data.remote.model.functions.SendApprovedEmailResponse
import com.siffmember.info.ui.fragment.ForgotPasswordBottomSheetFragment
import com.siffmember.info.ui.fragment.LoginWithPasswordBottomSheetFragment
import com.siffmember.info.utils.CommunityChat
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : BaseActivity(),
    LoginWithPasswordBottomSheetFragment.EmailPasswordBottomSheetListener, ForgotPasswordBottomSheetFragment.ForgotPasswordBottomSheetListener {

    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_SIGN_IN = 1001

    }

    private lateinit var binding: ActivityLoginBinding
    private var phoneNumberEntered = ""
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = Firebase.firestore
        auth = Firebase.auth
        credentialManager = CredentialManager.create(this)
        binding.apply {
            btnLogin.setOnClickListener {
                val number = etPhoneNumberRegister.text.toString().trim()
                if (number.isNotEmpty()) {
                    if(Utils.isNetworkAvailable(this@LoginActivity)){
                        val cc = ccp.selectedCountryCodeWithPlus
                        if(cc == "+91"){
                            val phoneNumber = "$cc$number"
                            phoneNumberEntered = phoneNumber
                            verifyPhoneIsAvailable(number, phoneNumber)
                        } else {
                            Toast.makeText(
                                this@LoginActivity,
                                "Please login with email and password",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Internet not available please try again later",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter phone number",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            btnGoogleLogin.setOnClickListener {
                if(Utils.isNetworkAvailable(this@LoginActivity)) {
                    launchGoogleSignIn()
                } else {
                    Toast.makeText(this@LoginActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
                }

            }
            btnLoginEp.setOnClickListener {
                if(Utils.isNetworkAvailable(this@LoginActivity)) {
                    val bottomSheetFragment = LoginWithPasswordBottomSheetFragment()
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                } else {
                    Toast.makeText(this@LoginActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
                }

            }
        }
    }

    private fun verifyPhoneIsAvailable(number: String, phoneNumber: String) {
        try {
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_USER_DETAILS).document(number)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.data != null) {
                        val retrievedToken = Utils.getResendToken(this@LoginActivity)
                        if (retrievedToken != null) {
                            reSendVerificationCode(phoneNumber, number)
                        } else {
                            sendVerificationCode(phoneNumber, number)
                        }
                    } else {
                        dismissProgDialog()
                        Toast.makeText(
                            this@LoginActivity,
                            "Please enter register phone number. If you are not register please contact support team.",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                }
                .addOnFailureListener { _ ->
                    Toast.makeText(
                        this@LoginActivity,
                        "Please enter register phone number. If you are not register please contact support team.",
                        Toast.LENGTH_LONG
                    ).show()
                    dismissProgDialog()
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sendVerificationCode(phoneNumber: String, number: String) {

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                //
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@LoginActivity, " $e", Toast.LENGTH_LONG).show()
                dismissProgDialog()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Utils.saveResendToken(this@LoginActivity, token)
                val next = Intent(this@LoginActivity, VerificationOtpActivity::class.java)
                next.putExtra(AppConstants.VERIFICATION_ID, verificationId)
                next.putExtra(AppConstants.PHONE_NUMBER, phoneNumberEntered)
                next.putExtra(AppConstants.PHONE_NUMBER_ONLY, number)
                startActivity(next)
                dismissProgDialog()
            }

        }
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun reSendVerificationCode(phoneNumber: String, number: String) {

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                //
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Toast.makeText(this@LoginActivity, " $e", Toast.LENGTH_LONG).show()
                dismissProgDialog()
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                val next = Intent(this@LoginActivity, VerificationOtpActivity::class.java)
                next.putExtra(AppConstants.VERIFICATION_ID, verificationId)
                next.putExtra(AppConstants.PHONE_NUMBER, phoneNumberEntered)
                next.putExtra(AppConstants.PHONE_NUMBER_ONLY, number)
                startActivity(next)
                dismissProgDialog()
            }

        }
        val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
            .setPhoneNumber(phoneNumber) // Phone number to verify
            .setTimeout(60L, TimeUnit.SECONDS) // Timeout and unit
            .setActivity(this) // Activity (for callback binding)
            .setCallbacks(callbacks) // OnVerificationStateChangedCallbacks
            .setForceResendingToken(Utils.getResendToken(this@LoginActivity)!!)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }


    private lateinit var googleSignInClient: GoogleSignInClient

    private fun launchGoogleSignIn() {
        try {
            val inputStream = resources.openRawResource(R.raw.client_secret_web)
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val clientId = jsonObject.getJSONObject("web").getString("client_id")

            val googleIdOption = GetGoogleIdOption.Builder()
                .setServerClientId(clientId)
                .setFilterByAuthorizedAccounts(false) // allow all accounts (important for Samsung)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            lifecycleScope.launch {
                try {
                    val result = credentialManager.getCredential(
                        context = this@LoginActivity,
                        request = request
                    )
                    handleSignIn(result.credential)
                } catch (e: GetCredentialException) {
                    Log.e(TAG, "Credential Manager failed: ${e.localizedMessage}")
                    // 🚨 Fallback to classic Google Sign-In
                    fallbackToGoogleSignIn(clientId)
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun fallbackToGoogleSignIn(clientId: String) {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            verifyEmailIsAvailable(googleIdTokenCredential.id) { isAvailable, phoneNumber ->
                if (isAvailable) {
                    firebaseAuthWithGoogle(googleIdTokenCredential.idToken, phoneNumber)
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "Please sign in with registered email. If you are not registered please contact support.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            Toast.makeText(
                this@LoginActivity,
                "Sign in failed, please try again!",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    @Deprecated("Use registerForActivityResult in new code")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                val idToken = account.idToken
                val email = account.email ?: ""
                showProgDialog()
                verifyEmailIsAvailable(email) { isAvailable, phoneNumber ->
                    dismissProgDialog()
                    if (isAvailable) {
                        firebaseAuthWithGoogle(idToken!!, phoneNumber)
                    } else {
                        Toast.makeText(
                            this@LoginActivity,
                            "Please sign in with registered email. If you are not registered please contact support.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

            } catch (e: ApiException) {
                Log.e(TAG, "Google Sign-In failed: ${e.statusCode}", e)
                Toast.makeText(this, "Google Sign-In failed, please try again!", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun verifyEmailIsAvailable(userEmail: String, callback: (Boolean, String) -> Unit) {
        try {
           // showProgDialog()
            db.collection(AppConstants.TABLE_USER_DETAILS)
                .whereEqualTo("email_id", userEmail)
                .limit(1) // Only need to check one match
                .get()
                .addOnSuccessListener { documents ->
                   // dismissProgDialog()
                    if (!documents.isEmpty) {
                        val doc = documents.documents.first()
                        val phoneNumber = doc.getString("phone_number") ?: ""
                        Log.e(TAG, "User is available")
                        callback(true, phoneNumber)
                    } else {
                        Log.e(TAG, "Email not found in database")
                        callback(false, "")
                    }
                }
                .addOnFailureListener { exception ->
                    //dismissProgDialog()
                    callback(false, "")
                    Log.e(TAG, "Error checking email: ${exception.message}")
                }

        } catch (e: Exception) {
           // dismissProgDialog()
            e.printStackTrace()
            callback(false, "")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String, phoneNumber: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    sharedPrefEditor.putBoolean(AppConstants.IS_LOGGEDIN, true).commit()
                    sharedPrefEditor.putString(AppConstants.USER_ID, phoneNumber).commit()
                    sharedPrefEditor.putBoolean(AppConstants.IS_LOGGEDIN_EP, false).commit()
                    addFCMToken(CommunityChat.getFCMToken(), phoneNumber)
                    val next = Intent(this@LoginActivity, HomeActivity::class.java)
                    startActivity(next)
                    finish()
                } else {
                    Log.e(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "Google Sign-In failed, please try again!", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    private fun addFCMToken(fcm: String, number: String) {
        if (fcm.isEmpty()) {
            Log.e(TAG, "⚠️ FCM token is empty, skipping update.")
            return
        }
        val docRef = db.collection(AppConstants.TABLE_FCM_DETAILS).document(number)
        docRef.get()
            .addOnSuccessListener { snapshot ->
                val existingToken = snapshot.getString("fcm")
                // 🔥 If token is same → no need to update
                if (existingToken == fcm) {
                    Log.e(TAG, "✅ FCM token unchanged, no update needed.")
                    dismissProgDialog()
                    return@addOnSuccessListener
                }
                // 🔥 Token changed OR doc does not exist → update now
                val data = hashMapOf(
                    "phone_number" to number,
                    "fcm" to fcm
                )
                docRef.set(data)
                    .addOnSuccessListener {
                        Log.e(TAG, "✅ FCM token updated successfully!")
                        dismissProgDialog()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "❌ Error updating token", e)
                        dismissProgDialog()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "❌ Error fetching document", e)
                dismissProgDialog()
            }
    }
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onLoginWithEmailPassword(emailId: String, password: String) {
        showProgDialog()
        verifyEmailIsAvailable(emailId) { isAvailable, phoneNumber ->
            if (isAvailable) {
                try {
                    FirebaseAuth.getInstance()
                        .signInWithEmailAndPassword(emailId, password)
                        .addOnCompleteListener { task ->
                            dismissProgDialog()
                            if (task.isSuccessful) {
                                val user = FirebaseAuth.getInstance().currentUser
                                if (user != null && user.isEmailVerified) {
                                    Log.d(TAG, "✅ Sign-in success and email verified.")
                                    // Proceed to main screen
                                    sharedPrefEditor.putBoolean(AppConstants.IS_LOGGEDIN, true).commit()
                                    sharedPrefEditor.putString(AppConstants.USER_ID, phoneNumber).commit()
                                    sharedPrefEditor.putBoolean(AppConstants.IS_LOGGEDIN_EP, true).commit()
                                    addFCMToken(CommunityChat.getFCMToken(), phoneNumber)
                                    val next = Intent(this@LoginActivity, HomeActivity::class.java)
                                    startActivity(next)
                                    finish()
                                } else {
                                    Log.e(TAG, "❌ Email not verified.")
                                    FirebaseAuth.getInstance().signOut()
                                    // Show alert to the user
                                    Toast.makeText(this@LoginActivity, "Please verify your email first.", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                Log.e(TAG, "signInWithEmail:failure: ${task.exception?.message}")
                                Toast.makeText(this@LoginActivity, "Please login with registered email. If you are not registered please contact support.", Toast.LENGTH_LONG).show()
                            }
                        }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } else {
                dismissProgDialog()
                Toast.makeText(
                    this@LoginActivity,
                    "Please sign in with registered email. If you are not registered please contact support.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onForgotPassword() {
        val bottomSheetFragment = ForgotPasswordBottomSheetFragment()
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    override fun onSendRestPassword(emailId: String) {
        showProgDialog()
        verifyEmailIsAvailable(emailId) { isAvailable, _ ->
            if (isAvailable) {
                try{
                    val request = ForgotPasswordRequest(emailId)
                    RetrofitInstanceFunction.api.forgotPasswordRequest(request)
                        .enqueue(object : Callback<SendApprovedEmailResponse> {
                            override fun onResponse(call: Call<SendApprovedEmailResponse>, response: Response<SendApprovedEmailResponse>) {
                                dismissProgDialog()
                                if (response.isSuccessful) {
                                    Log.e(TAG, "User account created successfully ${response.body()!!.message}")
                                    Toast.makeText(this@LoginActivity, response.body()!!.message, Toast.LENGTH_LONG).show()
                                } else {
                                    Log.e(TAG, "User account creation failed")
                                    Toast.makeText(this@LoginActivity, "User account creation failed", Toast.LENGTH_LONG).show()
                                }
                            }

                            override fun onFailure(call: Call<SendApprovedEmailResponse>, t: Throwable) {
                                Log.e(TAG, "Error: ${t.message}")
                                dismissProgDialog()
                                Toast.makeText(this@LoginActivity, "Failed to send reset password link, try again later.", Toast.LENGTH_LONG).show()

                            }
                        })
                } catch (e: Exception){
                    e.printStackTrace()
                }
            } else {
                dismissProgDialog()
                Toast.makeText(
                    this@LoginActivity,
                    "Please enter registered email. If you are not registered please contact support.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}