package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.CreateUserAccountRequest
import com.siffmember.info.data.remote.model.functions.SendApprovedEmailRequest
import com.siffmember.info.data.remote.model.functions.SendApprovedEmailResponse
import com.siffmember.info.databinding.ActivityApproveUserDetailsBinding
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.Users
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.UsersDetails
import com.siffmember.info.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ApproveUserDetailsActivity : BaseActivity() {

    companion object {
        var TAG = "ApproveUserDetailsActivity"
    }

    private lateinit var binding: ActivityApproveUserDetailsBinding
    private lateinit var db: FirebaseFirestore
    private var category = ""
    private var userName = ""
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityApproveUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        db = Firebase.firestore
        val adapter = ArrayAdapter(this, R.layout.spinner_list, Utils.category)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.apply {
            try {
                val users = UsersDetails.getUsersRegisteredDetails()
                userName = users.name!!
                muName.text = users.name
                muEmail.text = users.email_id
                muDofRegister.text = Utils.getTimeAgo(users.timestamp!!.toLong())
                val country = if(users.country!!.contains("+")){
                    users.country
                } else {
                    "+${users.country}"
                }
                if(users.phone_number!!.contains("+")){
                    muNumber.text = users.phone_number
                } else {
                    muNumber.text = "$country ${users.phone_number}"
                }
                muReferralCode.text = users.referralCode

                spinnerCategory.adapter = adapter

                spinnerCategory.setSelection(5)
                spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (view != null) {
                            category = Utils.category[position]
                        } else {
                            // handle the case where the view parameter is null
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // write code to perform some action
                    }
                }

                btnApproveUser.setOnClickListener {
                    getApproveCount { _, count ->
                        if(count<=250){
                            val user = Users(users.name, users.email_id, country, users.phone_number, category)
                            showProgDialog()
                            addUser(user)
                        } else {
                            Toast.makeText(this@ApproveUserDetailsActivity, "Today approve limit reached, try again tomorrow.", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                btnVerifyUser.setOnClickListener {
                    val intent = Intent(Intent.ACTION_DIAL)
                    intent.data = "tel:${users.phone_number}".toUri()
                    startActivity(intent)
                }

                deleteUser.setOnClickListener {
                    showProgDialog()
                    deleteUserDialog(users.phone_number)
                }
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    private fun addUser(user: Users){
        try{
            db.collection(AppConstants.TABLE_USER_DETAILS).document(user.phone_number!!)
                .set(user)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    val member = MembersGroup(user.phone_number, user.name!!, user.phone_number, false, "")
                    addMemberToGroup(member) { isAdded ->
                        if (isAdded) {
                            updateUserGroupDocument(member.phoneNumber) {
                                deleteUserDetails(user.phone_number){
                                    addApproveCount(user.phone_number) {
                                        if(user.country.equals("+91")){
                                            sendApproveEmail(user.email_id!!, "", "")
                                        } else {
                                            val tempPass = Utils.generateRandomPass(8)
                                            createUserAccount(user.email_id!!, tempPass)
                                        }
                                        dismissProgDialog()
                                        finish()
                                    }
                                }
                            }
                        } else {
                            dismissProgDialog()
                            //Toast.makeText(this, "Failed to add ${member.name}, try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    binding.spinnerCategory.setSelection(0)
                    dismissProgDialog()
                    Toast.makeText(this@ApproveUserDetailsActivity,"User approved successfully", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    Toast.makeText(this@ApproveUserDetailsActivity,"User failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun createUserAccount(emailId: String, userTempPass: String){
        try{
            val request = CreateUserAccountRequest(emailId, userTempPass)

            RetrofitInstanceFunction.api.createUserAccount(request)
                .enqueue(object : Callback<SendApprovedEmailResponse> {
                    override fun onResponse(call: Call<SendApprovedEmailResponse>, response: Response<SendApprovedEmailResponse>) {
                        dismissProgDialog()
                        if (response.isSuccessful) {
                            Log.e(TAG, "User account created successfully ${response.body()!!.message}")
                            sendApproveEmail(emailId, userTempPass, response.body()!!.message)
                        } else {
                            Log.e(TAG, "User account creation failed")
                        }
                    }

                    override fun onFailure(call: Call<SendApprovedEmailResponse>, t: Throwable) {
                        Log.e(TAG, "Error: ${t.message}")
                        dismissProgDialog()
                    }
                })
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun sendApproveEmail(emailId: String, userTempPass: String, verificationLink: String){
        try{
            val subject = "Your TribeOne Registration Has Been Approved"
            val msg = if(userTempPass.isEmpty()){
                """
                    Hello $userName,
            
                    We’re excited to let you know that your TribeOne registration request has been approved.
            
                    You can now log in to the TribeOne app using your registered phone number and OTP verification.
            
                    Welcome to the TribeOne community!
            
                    Best regards,
                    The TribeOne Team
                """.trimIndent()
            } else {
                """
                    Hello $userName,
            
                    We’re excited to let you know that your TribeOne registration request has been approved.
            
                    To complete your registration, please verify your email address using the link below:
                    $verificationLink
            
                    Once your email is verified, you can log in to the TribeOne app using your registered email address and the temporary password provided below:
            
                    Temporary Password: $userTempPass
            
                    You can change your password anytime from the app settings.
            
                    Welcome to the TribeOne community — we’re thrilled to have you onboard!
            
                    Best regards,
                    The TribeOne Team
                """.trimIndent()
            }

            val request = SendApprovedEmailRequest(emailId, subject, msg)

            RetrofitInstanceFunction.api.sendApprovedEmail(request)
                .enqueue(object : Callback<SendApprovedEmailResponse> {
                    override fun onResponse(call: Call<SendApprovedEmailResponse>, response: Response<SendApprovedEmailResponse>) {
                        dismissProgDialog()
                        if (response.isSuccessful) {
                            Log.e(TAG, "Email sent successfully")
                        } else {
                            Log.e(TAG, "Email sent failed")
                        }
                    }

                    override fun onFailure(call: Call<SendApprovedEmailResponse>, t: Throwable) {
                        Log.e(TAG, "Error: ${t.message}")
                        dismissProgDialog()
                    }
                })
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun addApproveCount(number: String, callback: (Boolean) -> Unit){
        try{
            db.collection(AppConstants.TABLE_USER_APPROVE_COUNT).document(number)
                .set(mapOf("approved" to true))
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    callback(true)
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    callback(false)
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun getApproveCount(callback: (Boolean, Int) -> Unit){
        try{
            db.collection(AppConstants.TABLE_USER_APPROVE_COUNT)
                .get()
                .addOnSuccessListener { documents ->
                    Log.e(TAG, "getApproveCount size : ${documents.size()}")
                    if (documents != null) {
                        callback(true, documents.size())
                    } else {
                        callback(false, 0)
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    callback(false, 0)
                }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun addMemberToGroup(newMember: MembersGroup, callback: (Boolean) -> Unit) {
        val userRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS)
        userRef.whereEqualTo("id", AppConstants.EDUCATION_GROUP_ID).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val currentMembers = (document["members"] as? List<Map<String, Any>>)?.map { memberMap ->
                            MembersGroup(
                                id = memberMap["id"] as? String ?: "",
                                name = memberMap["name"] as? String ?: "",
                                phoneNumber = memberMap["phoneNumber"] as? String ?: "",
                                isAdmin = memberMap["admin"] as? Boolean == true,
                                fcmToken = memberMap["fcmToken"] as? String ?: ""
                            )
                        } ?: emptyList()
                        // Check if the member already exists
//                        if (currentMembers.any { it.id == newMember.id }) {
//                            Log.e("Firestore", "Member already exists")
//                            callback(false)
//                            return@addOnSuccessListener
//                        }
                        // Add the new member to the list
                        val updatedMembers = currentMembers.toMutableList()
                        updatedMembers.add(newMember)
                        // Update Firestore
                        document.reference.update("members", updatedMembers)
                            .addOnSuccessListener {
                                Log.e("Firestore", "Member added successfully")
                                callback(true)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Error adding member", exception)
                                callback(false)
                            }
                    }
                } else {
                    Log.e("Firestore", "Group not found")
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error finding group", exception)
                callback(false)
            }
    }

    private fun updateUserGroupDocument(phoneNumber: String, onComplete: () -> Unit) {
        val userGroupRef = db.collection(AppConstants.TABLE_USER_GROUPS_DETAILS).document(phoneNumber)
        userGroupRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                userGroupRef.update("groupsId", FieldValue.arrayUnion(AppConstants.EDUCATION_GROUP_ID))
                    .addOnSuccessListener {
                        Log.d("Firestore", "Group ID added to existing document")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to update group ID", e)
                        onComplete()
                    }
            } else {
                val newUserGroup = mapOf("groupsId" to listOf(AppConstants.EDUCATION_GROUP_ID))
                userGroupRef.set(newUserGroup)
                    .addOnSuccessListener {
                        Log.d("Firestore", "New user group document created")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to create document", e)
                        onComplete()
                    }
            }
        }.addOnFailureListener { e ->
            Log.e("Firestore", "Failed to fetch document", e)
            onComplete()
        }
    }

    private fun deleteUserDialog(userId: String){
        try{
            AlertDialog.Builder(this)
                .setTitle("Delete User Alert")
                .setMessage("Are you sure you want to delete this user?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteUserDetails(userId) { success ->
                        if (success) {
                            Toast.makeText(this, "User deleted successfully", Toast.LENGTH_SHORT)
                                .show()
                            finish()
                        }
                        dialogInterface.dismiss()
                        dismissProgDialog()
                    }

                }
                .setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                    dismissProgDialog()
                }
                .create()
                .show()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }


    private fun deleteUserDetails(userId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_USER_REGISTRATION_DETAILS).document(userId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
                dismissProgDialog()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user details", e)
                onComplete(false)
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