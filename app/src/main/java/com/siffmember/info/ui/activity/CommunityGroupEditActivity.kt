package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.NotificationRequest
import com.siffmember.info.data.remote.model.functions.NotificationResponse
import com.siffmember.info.databinding.ActivityCommunityGroupEditBinding
import com.siffmember.info.ui.adapter.CommunityUserGroupAdapter
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.Utils
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import com.siffmember.info.ui.fragment.AddMemberBottomSheetFragment
import com.siffmember.info.ui.fragment.UpdateGroupNameBottomSheetFragment
import com.siffmember.info.ui.model.PostModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.ExcelUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommunityGroupEditActivity : BaseActivity(), CommunityUserGroupAdapter.CommunityUserGroupListener, AddMemberBottomSheetFragment.BottomSheetListener, UpdateGroupNameBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "CommunityGroupEditActivity"
    }

    private lateinit var binding: ActivityCommunityGroupEditBinding
    private var communityGroupId: String = ""
    private var communityGroupName: String = ""
    private var communityGroupDescription: String = ""
    private var communityGroupIcon: String = ""
    private var adminId: String = ""
    private var adminName: String = ""
    private var isGroupAdmin: Boolean = false
    private var isGroupActive: Boolean = false
    private lateinit var viewModel: CommunityViewModel
    private lateinit var postsViewModel: PostsMessageViewModel
    private lateinit var db: FirebaseFirestore

    private var userAdapter: CommunityUserGroupAdapter? = null
    private var userList: ArrayList<MembersGroup> = ArrayList()
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityGroupEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeaderGc) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // When keyboard opens -> push bottom layout up
            binding.groupEditSv.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        db = Firebase.firestore
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        setupAdapter()
        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                communityGroupId = vBundle.getString(AppConstants.COMMUNITY_GROUP_ID, null)
                communityGroupName = vBundle.getString(AppConstants.COMMUNITY_GROUP_NAME, null)
               // binding.groupNameTxt.text = communityGroupName
            }
            adminId = sharedPref.getString(AppConstants.USER_ID, null)!!
            adminName = sharedPref.getString(AppConstants.USER_NAME, null)!!
        } catch (e: Exception) {
            e.printStackTrace()
        }

        viewModel.getCommunitiesMemberById(communityGroupId).observe(this) { groupMember ->
            groupMember?.let {
                binding.groupDescription.text = it.description
                binding.groupNameTxt.text = it.groupName
                communityGroupDescription = groupMember.description
                communityGroupIcon = groupMember.groupIcon

                if(groupMember.createdBy == adminId){
                    isGroupAdmin = true
                }
                isGroupActive = it.groupStatus
                for(member in it.members){
                    if(member.phoneNumber == it.createdBy){
                        if(isGroupAdmin){
                            binding.groupBy.text = "Created by You, ${Utils.getGroupTime(it.createdAt.toLong())}"
                        } else {
                            binding.groupBy.text = "Created by ${member.name}, ${Utils.getGroupTime(it.createdAt.toLong())}"
                        }
                    }
                }
                binding.groupCount.text = "Group  • ${it.members.size} members"
                userList = it.members as ArrayList<MembersGroup>
                mSetPictureImageView(it.groupIcon)
                setupAdapter()

                Log.e(TAG, "Group status: $isGroupActive")
                if(isGroupAdmin){
                    binding.deleteGroup.visibility = View.VISIBLE
                    binding.exitGroup.visibility = View.GONE
                    binding.addUploadUsers.visibility = View.VISIBLE
                    binding.updateGroupName.visibility = View.VISIBLE
                    binding.listMemberTitle.visibility = View.GONE
                } else {
                    if(isGroupActive){
                        binding.deleteGroup.visibility = View.GONE
                        binding.exitGroup.visibility = View.VISIBLE
                        binding.addUploadUsers.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.VISIBLE else View.GONE
                        binding.listMemberTitle.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.GONE else View.VISIBLE
                    } else {
                        binding.deleteGroup.visibility = View.VISIBLE
                        binding.exitGroup.visibility = View.GONE
                        binding.addUploadUsers.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.VISIBLE else View.GONE
                        binding.listMemberTitle.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.GONE else View.VISIBLE
                    }
                    binding.updateGroupName.visibility = View.GONE
                }
            } ?: run {
                // Handle case where community is null (e.g., show a message)
            }
        }

        binding.apply {

            addMemberEdit.setOnClickListener {
                if(isGroupAdmin || sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) {
                    val bottomSheetFragment = AddMemberBottomSheetFragment()
                    val bundle = Bundle().apply {
                        putString("communityGroupId", communityGroupId)
                        putString("adminId", adminId)
                    }
                    bottomSheetFragment.arguments = bundle
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }
            }

            deleteGroup.setOnClickListener {
                if(isGroupActive) {
                    deleteGroupDialog()
                } else {
                    viewModel.deleteCommunitiesById(communityGroupId)
                    postsViewModel.deletePostMessagesByGroupId(communityGroupId)
                    postsViewModel.deleteReplyPostMessagesByGroupID(communityGroupId)
                    finish()
                }
            }

            exitGroup.setOnClickListener {
                exitGroupDialog()
            }

            profileImageEditBtn.setOnClickListener {
                selectImageFromGallery()
            }

            updateGroupName.setOnClickListener {
                if(isGroupAdmin) {
                    val bottomSheetFragment = UpdateGroupNameBottomSheetFragment()
                    val bundle = Bundle().apply {
                        putString("oldGroupName", communityGroupName)
                    }
                    bottomSheetFragment.arguments = bundle
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }
            }
            uploadUser.setOnClickListener {
                openFilePicker()
            }
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
        
    }

    // Function to launch the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/*"
        }
        fileSelectorLauncher.launch(intent)
    }

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                val filePath = saveImageToCache(uri)
                viewModel.updateFilePath(communityGroupId, filePath!!)
                mSetPictureImageView(filePath)
            }
        }
    }

    private fun mSetPictureImageView(filePath: String){
        Glide.with(this)
            .load(filePath)
            .placeholder(R.drawable.ic_group_place_holder)
            .error(R.drawable.ic_group_place_holder)
            .apply(RequestOptions.circleCropTransform())
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .skipMemoryCache(true)
            .into(binding.profileImage)
    }

    private fun selectImageFromGallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        selectImageLauncher.launch(intent)
    }

    private fun saveImageToCache(uri: Uri): String? {
        val fileName = getFileName(uri) ?: "$communityGroupId.jpg"
        val file = File(cacheDir, fileName)
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            return file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = null
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
        return name
    }

    private fun setupAdapter(){
        try{
            userList.sortWith(compareByDescending<MembersGroup> { it.id == adminId }
                .thenBy { it.name.lowercase() })
            val isAdmin = sharedPref.getBoolean(AppConstants.IS_ADMIN, false)
            val hasEditAccess = sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)
            val isEdit = isGroupAdmin || isAdmin || hasEditAccess
            userAdapter = CommunityUserGroupAdapter(isEdit, adminId, userList, this)
            binding.communityRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            binding.communityRv.adapter = userAdapter
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun exitGroupDialog(){
        try{
            android.app.AlertDialog.Builder(this)
                .setTitle("Exit group Alert")
                .setMessage("Are you sure you want to exit and delete this group?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    exitFromGroup()
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

    private fun deleteGroupDialog(){
        try{
            android.app.AlertDialog.Builder(this)
                .setTitle("Delete group Alert")
                .setMessage("Are you sure you want to delete this group?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteEmptyGroup()
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

    fun deleteEmptyGroup() {
        if (userList.size == 1 && userList[0].phoneNumber == adminId){
            showProgDialog()
            deleteGroupByGroupId(communityGroupId) { success ->
                if (success) {
                    removeGroupIdFromUser(adminId, communityGroupId) { isUserRemoved ->
                        if(isUserRemoved){
                            viewModel.deleteCommunitiesById(communityGroupId)
                            postsViewModel.deletePostMessagesByGroupId(communityGroupId)
                            postsViewModel.deleteReplyPostMessagesByGroupID(communityGroupId)
                            Toast.makeText(this, "Group deleted successfully", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                            finish()
                        } else {
                            Toast.makeText(this, "Failed to remove group, try again.", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                    }
                } else {
                    Toast.makeText(this, "Failed to delete group", Toast.LENGTH_SHORT).show()
                    dismissProgDialog()
                }
            }
        } else {
            Toast.makeText(this, "Remove members from the group", Toast.LENGTH_SHORT).show()
        }
    }
    fun deleteGroupByGroupId(groupId: String, callback: (Boolean) -> Unit) {
        try {
            val userRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS).document(groupId)
            userRef.delete()
                .addOnSuccessListener {
                    Log.d("Firestore", "Group deleted successfully")
                    callback(true) // Success
                }
                .addOnFailureListener { exception ->
                    Log.e("Firestore", "Error finding group", exception)
                    callback(false)
                }
        } catch (e: Exception) {
            e.printStackTrace()
            callback(false)
        }
    }


    override fun onUserSelectListener(users: MembersGroup) {
        Log.e(TAG, users.name)
        deleteMemberDialog(users)
    }

    private fun deleteMemberDialog(users: MembersGroup){
        try{
            android.app.AlertDialog.Builder(this)
                .setTitle("Remove Member Alert")
                .setMessage("Are you sure you want to remove ${users.name}?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    removeMember(users)
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

    fun postFirstMessage(groupName: String, groupId: String, timeStamp: String, postTitle: String, postDescription: String, onComplete: (Boolean, String) -> Unit){
        try{
            val groupDocRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId)
            groupDocRef.set(mapOf("placeholder" to true), SetOptions.merge())
            val userRef = groupDocRef.collection(AppConstants.TABLE_POST).document()
            val postData = PostModel(userRef.id, postTitle, postDescription, timeStamp, groupName, groupId, adminName, adminId)
            userRef.set(postData)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    onComplete(true, userRef.id)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    onComplete(false, userRef.id)
                }
        }catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    private fun removeMember(users: MembersGroup){
        showProgDialog()
        removeMemberFromGroup(users.id) { isRemoved ->
            if(isRemoved){
                val newUserList = ArrayList<MembersGroup>().apply {
                    addAll(userList)
                    removeAll { it.id == users.id }
                }
                removeGroupIdFromUser(users.id, communityGroupId) { isUserRemoved ->
                    if(isUserRemoved){
                        val createdAt = System.currentTimeMillis().toString()
                        postFirstMessage(communityGroupName, communityGroupId, createdAt, "$adminName removed ${users.name} from this group", "Admin removed ${users.name} from this group") { _, postId ->
                           // updateUserSyncPostStatus(postId, communityGroupId, adminId)
                            viewModel.updateGroupMembers(communityGroupId, newUserList)
                            val newMemberTokens = userList
                                .filter { it.id != adminId && it.fcmToken.isNotEmpty() }
                                .map { it.fcmToken }
                                .distinct()
                            sendNotificationRemove(
                                communityGroupName,
                                "$adminName removed ${users.name} from this group",
                                newMemberTokens,
                                "$adminName removed ${users.name} from this group",
                                "Admin removed ${users.name} from this group",
                                postId,
                                createdAt
                            )
                            postsViewModel.insertPostMessage(postId, "You removed ${users.name} from this group", communityGroupName, communityGroupId, "You removed ${users.name} from this group", createdAt, adminName, adminId)
                            Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show()
                        }
                       // viewModel.updateGroupMembers(communityGroupId, newUserList)
                       // Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show()
                        dismissProgDialog()
                    } else {
                        Toast.makeText(this, "Failed to remove group from user, try again.", Toast.LENGTH_SHORT).show()
                        dismissProgDialog()
                    }
                }

            } else {
                Toast.makeText(this, "Failed to remove member, try again.", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
        }

    }
    @Suppress("UNCHECKED_CAST")
    fun removeMemberFromGroup(memberId: String, callback: (Boolean) -> Unit) {
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
                        // Remove the member with matching ID
                        val updatedMembers = membersList.filter { it.id != memberId }
                        // Update Firestore
                        document.reference.update("members", updatedMembers)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Member removed successfully")
                                callback(true)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Error removing member", exception)
                                callback(false)
                            }
                    }
                } else {
                    Log.e("Firestore", "Group not found")
                    callback(true)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error finding group", exception)
                callback(false)
            }
    }

    fun exitFromGroup() {
        showProgDialog()
        removeGroupIdFromUser(adminId, communityGroupId) { isUserRemoved ->
            if(isUserRemoved){
                removeMemberFromGroup(adminId) { isRemoved ->
                    if (isRemoved) {
                        val createdAt = System.currentTimeMillis().toString()
                        postFirstMessage(
                            communityGroupName,
                            communityGroupId,
                            createdAt,
                            "$adminName exited from this group",
                            "$adminName exited from this group"
                        ) { _, postId ->
                           // updateUserSyncPostStatus(postId, communityGroupId, adminId)

                            viewModel.deleteCommunitiesById(communityGroupId)
                            postsViewModel.deletePostMessagesByGroupId(communityGroupId)
                            postsViewModel.deleteReplyPostMessagesByGroupID(communityGroupId)
                            Toast.makeText(
                                this,
                                "Exited from group and deleted successfully",
                                Toast.LENGTH_SHORT
                            ).show()
                            val newMemberTokens = userList
                                .filter { it.id != adminId && it.fcmToken.isNotEmpty() }
                                .map { it.fcmToken }
                                .distinct()
                            sendNotificationRemove(
                                communityGroupName,
                                "$adminName exited from this group",
                                newMemberTokens,
                                "$adminName exited from this group",
                                "$adminName exited from this group",
                                postId,
                                createdAt
                            )
                            dismissProgDialog()
                            finish()
                        }
                    }
                }
            } else {
                Toast.makeText(this, "Failed to exit from group, try again.", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
        }
    }

    private fun removeGroupIdFromUser(userId: String, groupId: String, onComplete: (Boolean) -> Unit) {
        val userGroupRef = FirebaseFirestore.getInstance()
            .collection(AppConstants.TABLE_USER_GROUPS_DETAILS)
            .document(userId)
        userGroupRef.update("groupsId", FieldValue.arrayRemove(groupId))
            .addOnSuccessListener {
                Log.d(TAG, "Successfully removed groupId from user_groups/$userId")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error removing groupId from user_groups/$userId", e)
                onComplete(false)
            }
    }

    override fun onAddMember(selectedUsersList: List<MembersGroup>) {
        showProgDialog()
        // Combine new members and existing members
        val newUserList = ArrayList<MembersGroup>().apply {
            addAll(selectedUsersList)
            addAll(userList)
        }
       // val addedNames = selectedUsersList.joinToString(", ") { it.name }

        val userRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS)
        userRef.whereEqualTo("id", communityGroupId).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents.first()
                    // ✅ Update Firestore members field in one go
                    document.reference.update("members", newUserList)
                        .addOnSuccessListener {
                            Log.d("Firestore", "✅ All members added successfully")
                            // ✅ Update all users' group documents
                            updateAllUserGroupDocs(selectedUsersList) {
                               // val createdAt = System.currentTimeMillis().toString()
                                // Update in ViewModel
                                viewModel.updateGroupMembers(communityGroupId, newUserList)
                                // ✅ Post system message once
//                                postFirstMessage(
//                                    communityGroupName,
//                                    communityGroupId,
//                                    createdAt,
//                                    "$adminName added new members in this group",
//                                    "Admin added $addedNames in this group"
//                                ) { _, postId ->
//                                  //  updateUserSyncPostStatus(postId, communityGroupId, adminId)
//                                    // ✅ Send FCM notifications once to all added users
//                                    sendNotificationsToUsers(selectedUsersList, communityGroupId, "$adminName added $addedNames in this group",
//                                        "Admin added $addedNames in this group", postId, createdAt) {
//                                        // Insert into local posts DB
//                                        postsViewModel.insertPostMessage(
//                                            postId,
//                                            "You added new members in this group",
//                                            communityGroupName,
//                                            communityGroupId,
//                                            "You added $addedNames in this group",
//                                            createdAt,
//                                            adminName,
//                                            adminId
//                                        )
//                                        Toast.makeText(
//                                            this,
//                                            "Members added successfully",
//                                            Toast.LENGTH_SHORT
//                                        ).show()
//                                        dismissProgDialog()
//                                    }
//                                }
                                Toast.makeText(
                                    this,
                                    "Members added successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dismissProgDialog()
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "❌ Failed to update members", e)
                            Toast.makeText(this, "Failed to add members", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                } else {
                    Log.e("Firestore", "❌ Group not found")
                    Toast.makeText(this, "Group not found", Toast.LENGTH_SHORT).show()
                    dismissProgDialog()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "❌ Error finding group", e)
                Toast.makeText(this, "Error adding members", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
    }

    private fun updateAllUserGroupDocs(selectedUsersList: List<MembersGroup>, onComplete: () -> Unit) {
        var completed = 0
        if (selectedUsersList.isEmpty()) {
            onComplete()
            return
        }

        selectedUsersList.forEach { member ->
            updateUserGroupDocument(member.phoneNumber) {
                completed++
                if (completed == selectedUsersList.size) {
                    onComplete()
                }
            }
        }
    }

    private fun updateUserGroupDocument(phoneNumber: String, onComplete: () -> Unit) {
        val userGroupRef = db.collection(AppConstants.TABLE_USER_GROUPS_DETAILS).document(phoneNumber)
        userGroupRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                userGroupRef.update("groupsId", FieldValue.arrayUnion(communityGroupId))
                    .addOnSuccessListener {
                        Log.d("Firestore", "Group ID added to existing document")
                        onComplete()
                    }
                    .addOnFailureListener { e ->
                        Log.e("Firestore", "Failed to update group ID", e)
                        onComplete()
                    }
            } else {
                val newUserGroup = mapOf("groupsId" to listOf(communityGroupId))
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
    /*private fun sendNotificationsToUsers(newMembers: List<MembersGroup>, groupId: String, postTitle: String, postContent: String, postId: String, timestamp: String, onComplete: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val newMemberTokens = newMembers
                    .filter { it.id != adminId && it.fcmToken.isNotEmpty() }
                    .map { it.fcmToken }
                    .distinct()
                val existingMemberTokens = userList.filter { user ->
                    newMembers.none { it.phoneNumber == user.phoneNumber } &&
                            user.phoneNumber != adminId &&
                            user.fcmToken.isNotEmpty()
                }.map { it.fcmToken }
                if (newMemberTokens.isNotEmpty()) {
                    sendNotification(
                        title = communityGroupName,
                        body = "You’ve been added to the group by $adminName",
                        tokens = newMemberTokens,
                        groupId = groupId,
                        postTitle = postTitle,
                        postContent = postContent,
                        postId = postId,
                        timestamp = timestamp

                    )
                }
                if (existingMemberTokens.isNotEmpty()) {
                    sendNotification(
                        title = communityGroupName,
                        body = "$adminName added new members",
                        tokens = newMemberTokens,
                        groupId = groupId,
                        postTitle = postTitle,
                        postContent = postContent,
                        postId = postId,
                        timestamp = timestamp
                    )
                }
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending notifications", e)
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }*/

    /*private fun sendNotification(title: String, body: String, tokens: List<String>, groupId: String, postTitle: String, postContent: String, postId: String, timestamp: String) {
        val notificationRequest = NotificationRequest(
            tokens = tokens,  // Replace with the actual FCM token
            title = title,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to "NewCommunity",
                "groupId" to groupId,
                "groupName" to title,
                "senderId" to adminId,
                "senderName" to adminName,
                "postTitle" to postTitle,
                "postId" to postId,
                "postContent" to postContent,
                "timestamp" to timestamp,
            )
        )
        RetrofitInstanceFunction.api.sendNotification(notificationRequest).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (response.isSuccessful) {
                    val notificationResponse = response.body()
                    if (notificationResponse?.success != null) {
                        // Notification sent successfully
                        Log.e(TAG,"Notification sent: ${notificationResponse.success}")
                    }
                } else {
                    // Handle error response
                    Log.e(TAG,"Error sending notification: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                // Handle failure
                Log.e(TAG,"Failed to send notification: ${t.message}")
            }
        })
    }*/

    private fun sendNotificationRemove(title: String, body: String, tokens: List<String>, postTitle: String, postContent: String, postId: String, timestamp: String) {
        val notificationRequest = NotificationRequest(
            tokens = tokens,  // Replace with the actual FCM token
            title = title,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to "NewCommunity",
                "groupId" to communityGroupId,
                "groupName" to title,
                "senderId" to adminId,
                "senderName" to adminName,
                "postTitle" to postTitle,
                "postId" to postId,
                "postContent" to postContent,
                "timestamp" to timestamp,
            )
        )
        RetrofitInstanceFunction.api.sendNotification(notificationRequest).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (response.isSuccessful) {
                    val notificationResponse = response.body()
                    if (notificationResponse?.success != null) {
                        // Notification sent successfully
                        Log.e(TAG,"Notification sent: ${notificationResponse.success}")
                    }
                } else {
                    // Handle error response
                    Log.e(TAG,"Error sending notification: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                // Handle failure
                Log.e(TAG,"Failed to send notification: ${t.message}")
            }
        })
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onUpdateGroupName(groupName: String) {
        if(communityGroupName != groupName){
            showProgDialog()
            updateGroupName(groupName){ success ->
                if (success){
                    viewModel.updateGroupDetails(
                        communityGroupId,
                        groupName,
                        communityGroupDescription,
                        communityGroupIcon
                    )
                    val newMemberTokens = userList
                        .filter { it.id != adminId && it.fcmToken.isNotEmpty() }
                        .map { it.fcmToken }
                        .distinct()
                    sendNotificationRemove(
                        communityGroupName,
                        "$adminName changed this group name to $groupName",
                        newMemberTokens,
                        "",
                        "",
                        "",
                        ""
                    )
                    binding.groupNameTxt.text = groupName
                    val resultIntent = Intent()
                    resultIntent.putExtra(AppConstants.COMMUNITY_GROUP_NAME, groupName)
                    setResult(RESULT_OK, resultIntent)
                    dismissProgDialog()
                } else {
                    dismissProgDialog()
                }

            }
        }
    }

    fun updateGroupName(newName: String, callback: (Boolean) -> Unit) {
        val userRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS)
        userRef.whereEqualTo("id", communityGroupId).get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    for (document in querySnapshot.documents) {
                        document.reference.update("name", newName)
                            .addOnSuccessListener {
                                Log.d("Firestore", "Group name updated successfully")
                                callback(true)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Firestore", "Error updating group name", exception)
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
    private var usersFromFileList: ArrayList<MembersGroup> = ArrayList()

    @OptIn(DelicateCoroutinesApi::class)
    private fun importExcelUsers(uri: Uri) {
        try {
            usersFromFileList.clear()
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "doInBackground: Importing...")
                    runOnUiThread {
                        Toast.makeText(this@CommunityGroupEditActivity, "Importing...", Toast.LENGTH_SHORT).show()
                    }
                    val readExcelNew: List<Map<Int, Any>> = ExcelUtil.readExcelNew(this@CommunityGroupEditActivity, uri, uri.path)
                    Log.i(TAG, "onActivityResult:readExcelNew: ${ readExcelNew.size} ")
                    if (readExcelNew.isNotEmpty()) {
                        Log.i(TAG, "run: successfully imported")
                        runOnUiThread {
                            Toast.makeText(this@CommunityGroupEditActivity, "successfully imported", Toast.LENGTH_SHORT).show()
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
                            val addedIds = userList.map { it.id }.toSet()
                            val filteredUsers = usersFromFileList.filter { user ->
                                (userList.isEmpty() || user.phoneNumber !in addedIds)
                            }
                            onAddMember(filteredUsers)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@CommunityGroupEditActivity, "No data available", Toast.LENGTH_SHORT).show()
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
}