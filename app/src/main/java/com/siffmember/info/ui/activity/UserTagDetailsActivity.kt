package com.siffmember.info.ui.activity

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityBlocklistDetailsBinding
import com.siffmember.info.ui.adapter.UserTagAdapter
import com.siffmember.info.ui.fragment.AddMeetingMemberBottomSheetFragment
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.TagUserListModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.ExcelUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UserTagDetailsActivity : BaseActivity(), UserTagAdapter.CommunityUserGroupListener, AddMeetingMemberBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "UserTagDetailsActivity"
    }

    private lateinit var binding: ActivityBlocklistDetailsBinding
    private lateinit var db: FirebaseFirestore
    private var hostName = ""
    private var hostID = ""
    private var tagListTitle = ""

    private var userAdapter: UserTagAdapter? = null
    private var userList: ArrayList<MembersGroup> = ArrayList()
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBlocklistDetailsBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        setupAdapter()
        binding.btnDeleteMeeting.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false)) View.VISIBLE else View.GONE
        db = Firebase.firestore
        hostName = sharedPref.getString(AppConstants.USER_NAME, null).toString()
        hostID = sharedPref.getString(AppConstants.USER_ID, null).toString()
        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                tagListTitle = vBundle.getString("tagListTitle", null)
            }
            binding.blTitle.text = tagListTitle
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.btnDeleteMeeting.setOnClickListener {
            deleteBlockedListDialog()
        }
        
        binding.addMemberEdit.setOnClickListener {
            val bottomSheetFragment = AddMeetingMemberBottomSheetFragment()
            val bundle = Bundle()
            bundle.putString("adminId", hostID)
            bottomSheetFragment.arguments = bundle
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }
        binding.uploadUser.setOnClickListener { 
            openFilePicker()
        }
        // Initialize the ActivityResultLauncher
        fileSelectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    //Log.e("AddUserActivity","${uri.path}")
                    showProgDialog()
                    importExcelUsers(uri)
                }

            }
        }
        fetchTaggedUsers()
    }

    // Function to launch the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/*"
        }
        fileSelectorLauncher.launch(intent)
    }
    
    @Suppress("UNCHECKED_CAST")

    private fun fetchTaggedUsers() {
        try {
            showProgDialog()
            db.collection(AppConstants.TABLE_USER_TAGS).document(tagListTitle)
                .get()
                .addOnSuccessListener { snapshot ->
                    val result = mutableListOf<MembersGroup>()
                    val rawList = snapshot.get("taggedUsers") as? List<HashMap<String, Any>> ?: emptyList()
                    val users = rawList.map {
                        MembersGroup(
                            id = it["id"] as? String ?: "",
                            name = it["name"] as? String ?: "",
                            phoneNumber = it["phoneNumber"] as? String ?: "",
                            fcmToken = it["fcmToken"] as? String ?: ""
                        )
                    }
                    result.addAll(users)
                    Log.e(TAG, "Tagged users count: ${result.size}")
                    userList = ArrayList(result)
                    setupAdapter()
                    dismissProgDialog()
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error fetching tagged users: ${it.message}")
                    dismissProgDialog()
                }

        } catch (e: Exception) {
            e.printStackTrace()
            dismissProgDialog()
        }
    }

    private fun setupAdapter(){
        try{
            userAdapter = UserTagAdapter(userList, this)
            binding.meetingUsersRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            binding.meetingUsersRv.adapter = userAdapter
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun removeMemberFromBlockedList(meetingId: String, memberId: MembersGroup, onComplete: (Boolean) -> Unit) {
        val meetingRef = db
            .collection(AppConstants.TABLE_USER_TAGS)
            .document(meetingId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(meetingRef)
                val meeting = snapshot.toObject(TagUserListModel::class.java)
                    ?: throw Exception("User not found")

                val updatedMemberIds = meeting.taggedUsers.filter { it != memberId }
                // 👆 assuming MembersZoomMeeting has `userId`
                transaction.update(meetingRef, mapOf(
                        "taggedUsers" to updatedMemberIds,
                    )
                )
            }
            .addOnSuccessListener {
                onComplete(true)
                dismissProgDialog()
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to remove member: ${it.message}")
                onComplete(false)
                dismissProgDialog()
            }
    }


    override fun onUserSelectListener(users: MembersGroup) {
        deleteMemberDialog(users)
    }

    private fun deleteMemberDialog(users: MembersGroup){
        try{
            AlertDialog.Builder(this)
                .setTitle("Remove Member Alert")
                .setMessage("Are you sure you want to remove ${users.name}?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    showProgDialog()
                    removeMemberFromBlockedList(tagListTitle, users) {
                        if(it){
                            Toast.makeText(this, "User removed successfully", Toast.LENGTH_SHORT).show()
                            fetchTaggedUsers()
                        } else {
                            Toast.makeText(this, "Failed to remove user, try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
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


    override fun onAddMember(selectedUsersList: List<MembersGroup>) {
        if (selectedUsersList.isEmpty()) {
            Toast.makeText(this, "Select at least one member", Toast.LENGTH_SHORT).show()
            return
        }
        showProgDialog()
        val meetingRef = db
            .collection(AppConstants.TABLE_USER_TAGS)
            .document(tagListTitle)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(meetingRef)
            if (!snapshot.exists()) {
                throw Exception("Document not found")
            }

            val meeting = snapshot.toObject(TagUserListModel::class.java)
            // Safe null handling
            val existingIds = meeting?.taggedUsers?.toMutableSet() ?: mutableSetOf()
            // Add new users without duplicates
            selectedUsersList.forEach { user ->
                 existingIds.add(user)
            }
            transaction.update(
                meetingRef,
                "taggedUsers", existingIds.toList()
            )
            existingIds
        }
            .addOnSuccessListener {
                dismissProgDialog()
                fetchTaggedUsers()
                Log.e(TAG, "Members added successfully")
            }
            .addOnFailureListener {
                dismissProgDialog()
                Log.e(TAG, "Failed to add members: ${it.message}")
                Toast.makeText(this, "Failed to add members", Toast.LENGTH_SHORT).show()
            }
    }
    private fun deleteBlockedListDialog() {
        try {
            AlertDialog.Builder(this)
                .setTitle("Delete Tag Alert")
                .setMessage("Are you sure you want to delete this tag?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteBlockListFireStore(tagListTitle) { isDeleted ->
                        if (isDeleted) {
                            finish()
                            dialogInterface.dismiss()
                        }
                    }
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteBlockListFireStore(meetingId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_USER_TAGS).document(meetingId)
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
    private var usersFromFileList: ArrayList<MembersGroup> = ArrayList()

    @OptIn(DelicateCoroutinesApi::class)
    private fun importExcelUsers(uri: Uri) {
        try {
            usersFromFileList.clear()
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "doInBackground: Importing...")
                    runOnUiThread {
                        Toast.makeText(this@UserTagDetailsActivity, "Importing...", Toast.LENGTH_SHORT).show()
                    }
                    val readExcelNew: List<Map<Int, Any>> = ExcelUtil.readExcelNew(this@UserTagDetailsActivity, uri, uri.path)
                    Log.i(TAG, "onActivityResult:readExcelNew: ${ readExcelNew.size} ")
                    if (readExcelNew.isNotEmpty()) {
                        Log.i(TAG, "run: successfully imported")
                        runOnUiThread {
                            Toast.makeText(this@UserTagDetailsActivity, "successfully imported", Toast.LENGTH_SHORT).show()
                        }

                        for (i in readExcelNew.indices) {
                            val map = readExcelNew[i]
                            val name = map[0]?.toString() ?: ""
                            //val email = map[1]?.toString() ?: ""
                            //val country = map[2]?.toString() ?: ""
                            val phoneNumber = map[3]?.toString() ?: ""
                            //val category = map[4]?.toString() ?: ""

                            if (name.isNotEmpty() && name != "Name" && phoneNumber.isNotEmpty()) {
                                val user = MembersGroup(phoneNumber, name, phoneNumber, false, "")
                                usersFromFileList.add(user)
                            }
                        }
                        if(usersFromFileList.isNotEmpty()){
                            Log.e(TAG, " Users list:: ${usersFromFileList.size}")
                            val existingNumbers  = userList.map { it.phoneNumber }.toSet()
                            val filteredUsers = usersFromFileList.filter { user ->
                                user.phoneNumber.isNotEmpty() && user.phoneNumber !in existingNumbers
                            }
                            onAddMember(filteredUsers)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@UserTagDetailsActivity, "No data available", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
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