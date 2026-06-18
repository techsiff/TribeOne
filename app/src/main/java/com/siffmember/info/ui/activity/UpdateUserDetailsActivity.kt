package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityUpdateUserDetailsBinding
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CallLogDetails
import com.siffmember.info.utils.PermissionUtils
import com.siffmember.info.utils.UsersDetails
import com.siffmember.info.utils.Utils

class UpdateUserDetailsActivity : BaseActivity() {

    companion object {
        var TAG = "UpdateUserDetailsActivity"
    }

    private lateinit var binding: ActivityUpdateUserDetailsBinding
    private lateinit var db: FirebaseFirestore
    private var category = ""
    private var memberId = ""
    private var adminId: String = ""
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpdateUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        db = Firebase.firestore
        adminId = sharedPref.getString(AppConstants.USER_ID, null)!!
        val adapter = ArrayAdapter(this, R.layout.spinner_list, Utils.category)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.apply {
            btnUpdateUser.visibility = View.GONE
            btnUpdateMemberParam.visibility = View.GONE
            try {
                val users = UsersDetails.getUsersDetails()
                btnCallUser.visibility = if (adminId == users.phone_number) View.GONE else View.VISIBLE
                deleteUser.visibility = if (adminId == users.phone_number) View.GONE else View.VISIBLE
                muName.text = users.name
                muEmail.text = users.email_id
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
                spinnerCategory.adapter = adapter
                spinnerCategory.setSelection(Utils.category.indexOf(users.category))
                spinnerCategory.isEnabled = adminId != users.phone_number
                spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        if (view != null) {
                            category = Utils.category[position]
                            btnUpdateUser.visibility = if (category == users.category) View.GONE else View.VISIBLE
                        } else {
                            // handle the case where the view parameter is null
                        }
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        // write code to perform some action
                        btnUpdateUser.visibility = View.GONE
                    }
                }
                btnUpdateUser.setOnClickListener {
                    showProgDialog()
                    updateUserCategory(users.phone_number, category)
                }
                btnUpdateMemberParam.setOnClickListener {
                    val intent = Intent(this@UpdateUserDetailsActivity, MembershipAllDetailsActivity::class.java)
                    intent.putExtra("memberId", memberId)
                    startActivity(intent)
                }
                deleteUser.setOnClickListener {
                    showProgDialog()
                    deleteUserDialog(users.phone_number)
                }
                btnCallUser.setOnClickListener {
                    if (!PermissionUtils.hasAllPermissions(this@UpdateUserDetailsActivity)) {
                        permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
                    } else {
                        users.let {
                            CallLogDetails.setCallInitiated(true)
                            CallLogDetails.setUserName(users.name!!)
                            CallLogDetails.setUserPhoneNumber(users.phone_number)
                            val intent = Intent(Intent.ACTION_CALL)
                            intent.data = "tel:${it.phone_number}".toUri()
                            startActivity(intent)
                        }
                    }
                }
                btnHistoryUser.setOnClickListener {
                    val next = Intent(this@UpdateUserDetailsActivity, UserCallHistoryActivity::class.java)
                    next.putExtra("contact_name", users.name!!)
                    next.putExtra("contact_number", users.phone_number)
                    next.putExtra("screen", "2")
                    startActivity(next)
                }
                val lastFour = users.phone_number.takeLast(4)
                searchByParam(lastFour)
            } catch (e: Exception){
                e.printStackTrace()
            }
        }
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                //var allGranted = true
                permissions.entries.forEach { entry ->
                    val permission = entry.key
                    val isGranted = entry.value
                    if (!isGranted) {
                        //allGranted = false
                        if (PermissionUtils.isPermissionDeniedForever(this, permission)) {
                            showGoToSettingsDialog()
                        } else {
                            showPermissionDeniedDialog()
                        }
                    }
                }
                /*if (allGranted) {
                    onAllPermissionsGranted()
                }*/
            }
        requestPermissions()
        CallLogDetails.setCallInitiated(false)
        CallLogDetails.setUserName("")
        CallLogDetails.setUserPhoneNumber("")
    }

    private fun searchByParam(value: String) {
        showProgDialog()
        val fieldPath = "Personal Info.PhoneLast4"
        db.collection(AppConstants.TABLE_MEMBERSHIP_MEMBER_DETAILS)
            .whereEqualTo(fieldPath, value)
            .get()
            .addOnSuccessListener { querySnapshot ->
                dismissProgDialog()
                if (querySnapshot.isEmpty) {
                    binding.btnUpdateMemberParam.visibility = View.GONE
                    //Toast.makeText(this, "Membership data not available.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }
                val document = querySnapshot.documents[0]
                val data = document.data
                if (data != null) {
                    binding.btnUpdateMemberParam.visibility = View.VISIBLE
                    memberId = document.id
                }
            }
            .addOnFailureListener {
                dismissProgDialog()
                binding.btnUpdateMemberParam.visibility = View.GONE
                //Toast.makeText(this, "Search failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun requestPermissions() {
        if (!PermissionUtils.hasAllPermissions(this)) {
            permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
        } /*else {
            onAllPermissionsGranted()
        }*/
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("These permissions are required for call monitoring. Please allow them.")
            .setPositiveButton("Retry") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("You have permanently denied permissions. Please enable them in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun updateUserCategory(userId: String, userCategory: String){
        try{
            db.collection(AppConstants.TABLE_USER_DETAILS).document(userId)
                .update("category", userCategory)
                .addOnSuccessListener {
                    Toast.makeText(this@UpdateUserDetailsActivity,"User details updated successfully", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                    UsersDetails.getUsersDetails().category = userCategory
                    binding.btnUpdateUser.visibility = View.GONE
                    UsersDetails.setIsEditUserDetails(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@UpdateUserDetailsActivity,"Failed to update try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }

        }catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    private fun deleteUserDialog(userId: String){
        try{
            AlertDialog.Builder(this)
                .setTitle("Delete User Alert")
                .setMessage("Are you sure you want to delete this user?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteUserData(userId)
                    dialogInterface.dismiss()
                }
                .setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun deleteUserData(userId: String) {
        fetchUserGroups(userId) { success ->
            if (!success) return@fetchUserGroups handleFailure()

            deleteUserDetails(userId) { detailsDeleted ->
                if (!detailsDeleted) return@deleteUserDetails handleFailure()

                deleteUserGroup(userId) { groupDeleted ->
                    if (!groupDeleted) return@deleteUserGroup handleFailure()

                    deleteUserFCM(userId) { fcmDeleted ->
                        if (fcmDeleted) {
                            Toast.makeText(this@UpdateUserDetailsActivity, "User deleted successfully", Toast.LENGTH_LONG).show()
                            dismissProgDialog()
                            UsersDetails.setIsEditUserDetails(true)
                            finish()
                        } else {
                            handleFailure()
                        }
                    }
                }
            }
        }
    }

    private fun handleFailure() {
        Toast.makeText(
            this@UpdateUserDetailsActivity,
            "Failed to delete user, try again!",
            Toast.LENGTH_LONG
        ).show()
        dismissProgDialog()
    }

    private fun deleteUserDetails(userId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_USER_DETAILS).document(userId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user details", e)
                onComplete(false)
            }
    }

    private fun deleteUserGroup(userId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_USER_GROUPS_DETAILS).document(userId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user group", e)
                onComplete(false)
            }
    }

    private fun deleteUserFCM(userId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_FCM_DETAILS).document(userId)
            .delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user FCM", e)
                onComplete(false)
            }
    }


    @Suppress("UNCHECKED_CAST")
    fun fetchUserGroups(userId: String, onComplete: (Boolean) -> Unit) {
        val userGroupRef = db.collection(AppConstants.TABLE_USER_GROUPS_DETAILS).document(userId)
        userGroupRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val groupsIdList = document.get("groupsId") as? List<String> ?: emptyList()
                    if (groupsIdList.isNotEmpty()) {
                        var completedCount = 0
                        val total = groupsIdList.size
                        var allSuccess = true
                        groupsIdList.forEach { groupId ->
                            removeMemberFromGroup(userId, groupId) { success ->
                                if (!success) allSuccess = false
                                completedCount++
                                if (completedCount == total) {
                                    onComplete(allSuccess)
                                }
                            }
                        }
                    } else {
                        Log.e("Firestore", "User is not part of any group")
                        onComplete(false) // Return empty list if no groups
                    }
                } else {
                    Log.e("Firestore", "No document found for user: $userId")
                    onComplete(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching user groups", e)
                onComplete(false) // Return empty list on failure
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun removeMemberFromGroup(memberId: String, communityGroupId: String, onComplete: (Boolean) -> Unit) {
        val userRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS)
        userRef.whereEqualTo("id", communityGroupId).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        val membersList = (document["members"] as? List<Map<String, Any>>)?.map { memberMap ->
                            MembersGroup(
                                id = memberMap["id"] as? String ?: "",
                                name = memberMap["name"] as? String ?: "",
                                phoneNumber = memberMap["phoneNumber"] as? String ?: "",
                                isAdmin = memberMap["admin"] as? Boolean == true,
                                fcmToken = memberMap["fcmToken"] as? String ?: ""
                            )
                        } ?: emptyList()

                        val updatedMembers = membersList.filter { it.id != memberId }

                        document.reference.update("members", updatedMembers)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Member removed successfully")
                                onComplete(true)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Error removing member", exception)
                                onComplete(false)
                            }
                    }
                } else {
                    Log.e("Firestore", "Group not found")
                    onComplete(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error finding group", exception)
                onComplete(false)
            }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
             CallLogDetails.setCallInitiated(false)
             CallLogDetails.setUserName("")
             CallLogDetails.setUserPhoneNumber("")
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}