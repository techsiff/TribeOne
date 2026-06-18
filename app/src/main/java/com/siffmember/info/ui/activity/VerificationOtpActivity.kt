package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.utils.VerificationOTPValidation
import com.siffmember.info.utils.validOTP
import com.siffmember.info.databinding.ActivityVerificationOtpBinding
import com.siffmember.info.ui.activity.HomeActivity.Companion.TAG
import com.siffmember.info.ui.view.otp.OTPListener
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat
import com.siffmember.info.utils.Utils

class VerificationOtpActivity : BaseActivity() {

    private lateinit var binding: ActivityVerificationOtpBinding
    private var vOTP: String = ""
    private var verificationId: String = ""
    private var phoneNumber: String = ""
    private var phoneNumberOnly: String = ""
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = Firebase.firestore
        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                verificationId = vBundle.getString(AppConstants.VERIFICATION_ID, null)
                phoneNumber = vBundle.getString(AppConstants.PHONE_NUMBER, null)
                phoneNumberOnly = vBundle.getString(AppConstants.PHONE_NUMBER_ONLY, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.apply {
            btnVerify.setOnClickListener {
                if(Utils.isNetworkAvailable(this@VerificationOtpActivity)) {
                    if(vOTP.isNotEmpty()) {
                        val smsCode = vOTP
                        try {
                            signInWithVerificationCode(verificationId, smsCode)
                        }catch (e: Exception){
                            e.printStackTrace()
                        }
                    } else {
                        Toast.makeText(this@VerificationOtpActivity,"Please enter verification code", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@VerificationOtpActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
                }
            }
            otpView.requestFocusOTP()
            otpView.otpListener = object : OTPListener {
                override fun onInteractionListener() {
                    //
                }

                override fun onOTPComplete(otp: String) {
                    vOTP = otp
                }
            }
            numberText.text = phoneNumber

            backToPhone.setOnClickListener {
                finish()
            }
        }
    }

    private fun signInWithVerificationCode(verificationId: String, codeSendVerification: String) {
        showProgDialog()
        val credential = PhoneAuthProvider.getCredential(verificationId, codeSendVerification)
        signInWithPhoneAuthCredential(credential, codeSendVerification)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, codeSendVerification: String) {
        if (checkValidation(codeSendVerification)) {
            FirebaseAuth.getInstance().signInWithCredential(credential)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        sharedPrefEditor.putBoolean(AppConstants.IS_LOGGEDIN, true).commit()
                        sharedPrefEditor.putString(AppConstants.USER_ID, phoneNumberOnly).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_LOGGEDIN_EP, false).commit()
                        addFCMToken(CommunityChat.getFCMToken(), phoneNumberOnly)
                        val next = Intent(this@VerificationOtpActivity, HomeActivity::class.java)
                        startActivity(next)
                        finish()
                    }
                    dismissProgDialog()
                }
        }
    }

    private fun checkValidation(codeSendVerification: String): Boolean {
        val otpValidation = validOTP(codeSendVerification)
        return (otpValidation is VerificationOTPValidation.Success)
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
}