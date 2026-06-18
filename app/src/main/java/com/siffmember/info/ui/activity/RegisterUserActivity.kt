package com.siffmember.info.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityRegisterUserBinding
import com.siffmember.info.ui.model.UsersRegistration
import com.siffmember.info.utils.AppConstants

class RegisterUserActivity : BaseActivity() {

    companion object {
        var TAG = "RegisterUserActivity"
    }
    private lateinit var binding: ActivityRegisterUserBinding
    private lateinit var db: FirebaseFirestore
    private var nameFN = ""
    private var nameLN = ""
    private var email = ""
    private var phone = ""
    private var referral = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterUserBinding.inflate(layoutInflater)
        setContentView(binding.root)
        db = Firebase.firestore
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        binding.apply {
            btnAdd.setOnClickListener {
                if(validate()) {
                    verifyPhoneIsAvailable(phone){ success ->
                        if (success){
                            Toast.makeText(this@RegisterUserActivity,"Phone number already registered", Toast.LENGTH_LONG).show()
                        } else {
                            verifyEmailIsAvailable(email) { success ->
                                if (success) {
                                    Toast.makeText(this@RegisterUserActivity, "Email id already used enter different email id", Toast.LENGTH_LONG).show()
                                } else {
                                    verifyIsRequestSent(phone){ success ->
                                        if (success){
                                            Toast.makeText(this@RegisterUserActivity,"This registration request already sent to SIFF team and waiting for approval", Toast.LENGTH_LONG).show()
                                        } else {
                                            val nameFL = "$nameFN $nameLN"
                                            val cc = ccp.selectedCountryCodeWithPlus
                                            val timestamp = System.currentTimeMillis().toString()
                                            val user = UsersRegistration(nameFL, email, cc, phone, referral ,timestamp)
                                            showProgDialog()
                                            addUser(user)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun validate(): Boolean{
        if(binding.nameEditFn.text.toString().trim().isNotEmpty()){
            nameFN = binding.nameEditFn.text.toString()
        } else {
            Toast.makeText(this@RegisterUserActivity,"Please enter first name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.nameEditLn.text.toString().trim().isNotEmpty()){
            nameLN = binding.nameEditLn.text.toString()
        } else {
            Toast.makeText(this@RegisterUserActivity,"Please enter last name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.emailEdit.text.toString().trim().isNotEmpty()){
            email = binding.emailEdit.text.toString()
        } else {
            Toast.makeText(this@RegisterUserActivity,"Please enter email id", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.phoneEdit.text.toString().trim().isNotEmpty()){
            phone = binding.phoneEdit.text.toString()
        } else {
            Toast.makeText(this@RegisterUserActivity,"Please enter phone number", Toast.LENGTH_LONG).show()
            return false
        }
        referral = binding.referralCode.text.toString().ifEmpty {
            ""
        }
        /*if(binding.referralCode.text.toString().isNotEmpty()){
            referral = binding.referralCode.text.toString()
        } else {
            Toast.makeText(this@RegisterUserActivity,"Please enter referralCode code", Toast.LENGTH_LONG).show()
            return false
        }*/
        return true
    }
    private fun verifyPhoneIsAvailable(number: String, callback: (Boolean) -> Unit){
        try{
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_USER_DETAILS).document(number)
            docRef.get()
                .addOnSuccessListener { document ->
                    dismissProgDialog()
                    if (document.data != null) {
                        Log.e(TAG, "phone number is available")
                        callback(true)
                    } else {
                        Log.e(TAG, "phone number not found in database")
                        callback(false)
                    }
                }
                .addOnFailureListener { exception ->
                    dismissProgDialog()
                    Log.e(TAG, "Error checking email: ${exception.message}")
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    private fun verifyEmailIsAvailable(userEmail: String, callback: (Boolean) -> Unit) {
        try {
            showProgDialog()
            db.collection(AppConstants.TABLE_USER_DETAILS)
                .whereEqualTo("email_id", userEmail)
                .limit(1) // Only need to check one match
                .get()
                .addOnSuccessListener { documents ->
                    dismissProgDialog()
                    if (!documents.isEmpty) {
                        Log.e(TAG, "Email is available")
                        callback(true)
                    } else {
                        Log.e(TAG, "Email not found in database")
                        callback(false)
                    }
                }
                .addOnFailureListener { exception ->
                    dismissProgDialog()
                    Log.e(TAG, "Error checking email: ${exception.message}")
                }

        } catch (e: Exception) {
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    private fun verifyIsRequestSent(phoneNumber: String,callback: (Boolean) -> Unit) {
        try {
            showProgDialog()
            try{
                showProgDialog()
                val docRef = db.collection(AppConstants.TABLE_USER_REGISTRATION_DETAILS).document(phoneNumber)
                docRef.get()
                    .addOnSuccessListener { document ->
                        dismissProgDialog()
                        if (document.data != null) {
                            Log.e(TAG, "Request sent")
                            callback(true)
                        } else {
                            Log.e(TAG, "Request not found in database")
                            callback(false)
                        }
                    }
                    .addOnFailureListener { exception ->
                        dismissProgDialog()
                        Log.e(TAG, "Error checking email: ${exception.message}")
                    }
            }catch (e: Exception){
                e.printStackTrace()
            }
        } catch (e: Exception) {
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    private fun addUser(user: UsersRegistration){
        try{
            db.collection(AppConstants.TABLE_USER_REGISTRATION_DETAILS).document(user.phone_number!!)
                .set(user)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    binding.nameEditFn.setText("")
                    binding.nameEditLn.setText("")
                    binding.emailEdit.setText("")
                    binding.phoneEdit.setText("")
                    binding.referralCode.setText("")
                    nameFN = ""
                    nameLN = ""
                    email = ""
                    phone = ""
                    referral = ""
                    dismissProgDialog()
                    Toast.makeText(this@RegisterUserActivity,"Registration request sent successfully SIFF team will verify and contact you soon then you can login", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    Toast.makeText(this@RegisterUserActivity,"Registration failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
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