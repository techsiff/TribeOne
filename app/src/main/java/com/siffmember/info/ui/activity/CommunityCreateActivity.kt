package com.siffmember.info.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.NotificationRequest
import com.siffmember.info.data.remote.model.functions.NotificationResponse
import com.siffmember.info.databinding.ActivityCommunityCreateBinding
import com.siffmember.info.ui.fragment.AddMemberBottomSheetFragment
import com.siffmember.info.ui.model.Community
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.PostModel
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat
import com.siffmember.info.utils.ExcelUtil
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CommunityCreateActivity : BaseActivity(), AddMemberBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "CommunityCreateActivity"
    }

    private lateinit var binding: ActivityCommunityCreateBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var viewModel: CommunityViewModel
    private lateinit var postsViewModel: PostsMessageViewModel
    private var topicName = ""
    private var description = ""
    private var adminName = ""
    private var adminID = ""
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.communityRl) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }

        db = Firebase.firestore
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        try {
            adminName = sharedPref.getString(AppConstants.USER_NAME, null)!!
            adminID = sharedPref.getString(AppConstants.USER_ID, null)!!
        }catch (e: Exception){
            e.printStackTrace()
        }

        binding.btnAddMembers.setOnClickListener {
            if (validate()) {
                val bottomSheetFragment = AddMemberBottomSheetFragment()
                val bundle = Bundle().apply {
                    putString("communityGroupId", "")
                    putString("adminId", adminID)
                }
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }
        binding.btnUploadMembers.setOnClickListener {
            if (validate()) {
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

    fun postFirstMessage(groupName: String, groupId: String, timeStamp: String, userName: String, userId: String, onComplete: (Boolean, String) -> Unit){
        try{
            val groupDocRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId)
            groupDocRef.set(mapOf("placeholder" to true), SetOptions.merge())
            val userRef = groupDocRef.collection(AppConstants.TABLE_POST).document()
            val postData = PostModel(userRef.id, "$adminName created this group", "You added in this group", timeStamp, groupName, groupId, userName, userId)
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

    fun createCommunity(name: String, description: String, members: List<MembersGroup>, adminId: String, adminName: String, createdAt: String, callback: (Boolean, String) -> Unit) {
        val userRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS).document()  // Store groups under admin user
        val updatedMembers = members.toMutableList().apply {
            if (none { it.id == adminId }) {
                add(MembersGroup(adminId, adminName, adminId, true, CommunityChat.getFCMToken()))
            }
        }
        val community = Community(
            id = userRef.id,
            name = name,
            description = description,
            createdBy = adminId,
            createdAt = createdAt,
            members = updatedMembers
        )
        // Firestore batch write for atomic operations
        val batch = db.batch()
        batch.set(userRef, community)
        // Commit batch operation
        batch.commit()
            .addOnSuccessListener {
                updatedMembers.forEach { userId ->
                    val userGroupRef = db.collection(AppConstants.TABLE_USER_GROUPS_DETAILS).document(userId.phoneNumber)
                    userGroupRef.get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Document exists → Just update array field
                            userGroupRef.update("groupsId", FieldValue.arrayUnion(userRef.id))
                                .addOnSuccessListener {
                                    Log.e("Firestore", "Group ID added successfully")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error updating group ID", e)
                                }
                        } else {
                            //  Document doesn't exist → Create it first
                            val newUserGroup = mapOf(
                                "groupsId" to listOf(userRef.id)  // Start with the new group ID
                            )
                            userGroupRef.set(newUserGroup)
                                .addOnSuccessListener {
                                    Log.d("Firestore", "New document created and Group ID added")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("Firestore", "Error creating document", e)
                                }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("Firestore", "Error fetching document", e)
                    }
                }
                callback(true, userRef.id)

            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseHelper", "Error creating community", exception)
                callback(false, userRef.id)
            }
    }

    private fun sendNotification(title: String, body: String, tokens: List<String>, groupId: String, postId: String, postTitle: String, postContent: String, timestamp: String, onComplete: (Boolean) -> Unit) {
        val senderName = sharedPref.getString(AppConstants.USER_NAME, null)
        val senderID = sharedPref.getString(AppConstants.USER_ID, null)

        val notificationRequest = NotificationRequest(
            tokens = tokens,  // Replace with the actual FCM token
            title = title,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to "NewCommunity",
                "groupId" to groupId,
                "groupName" to title,
                "senderId" to senderID.toString(),
                "senderName" to senderName.toString(),
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
                        onComplete(true)
                    }
                } else {
                    // Handle error response
                    Log.e(TAG,"Error sending notification: ${response.errorBody()}")
                    onComplete(false)
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                // Handle failure
                Log.e(TAG,"Failed to send notification: ${t.message}")
                onComplete(false)
            }
        })
    }

    private fun validate(): Boolean{
        if(binding.etCommunityName.text.toString().isNotEmpty()){
            topicName = binding.etCommunityName.text.toString()
        } else {
            Toast.makeText(this@CommunityCreateActivity,"Please enter group title", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.etCommunityDescription.text.toString().isNotEmpty()){
            description = binding.etCommunityDescription.text.toString()
        } else {
            Toast.makeText(this@CommunityCreateActivity,"Please enter group description", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onAddMember(selectedUsersList: List<MembersGroup>) {
        if(selectedUsersList.isNotEmpty()){
            showProgDialog()
            val createdAt = System.currentTimeMillis().toString()
            createCommunity(topicName, description, selectedUsersList, adminID, adminName, createdAt) { success, id ->
                if (success) {
                    // Start sending notifications to all selected users
                    postFirstMessage(topicName, id, createdAt, adminName, adminID) { _, postId ->
                        //updateUserSyncPostStatus(postId, id, adminID)
                        val newMemberTokens = selectedUsersList
                            .filter { it.id != adminID && it.fcmToken.isNotEmpty() }
                            .map { it.fcmToken }
                            .distinct()
                        sendNotification(topicName, "$adminName added you", tokens = newMemberTokens,
                            id, postId, "$adminName created this group", "You added in this group", createdAt) { isSent ->
                            if(isSent) {
                                val updatedMembers = selectedUsersList.toMutableList().apply {
                                    if (none { it.id == adminID }) {
                                        add(MembersGroup(adminID, adminName, adminID, true, CommunityChat.getFCMToken()))
                                    }
                                }
                                lifecycleScope.launch {
                                    viewModel.insertCommunity(
                                        CommunityEntity(
                                            id, topicName, description, adminID,
                                            createdAt, "", true, updatedMembers
                                        )
                                    )

                                    postsViewModel.insertPostMessage(
                                        postId, "You created this group",
                                        topicName, id, "Created new group",
                                        createdAt, adminName, adminID
                                    )

                                    // Now safe to finish after DB insert
                                    Toast.makeText(this@CommunityCreateActivity, "Community created successfully", Toast.LENGTH_SHORT).show()
                                    dismissProgDialog()
                                    finish()
                                   /* Handler(Looper.getMainLooper()).postDelayed({
                                        dismissProgDialog()
                                        finish()
                                    }, 500) // 500ms delay*/
                                }
                            } else {
                                dismissProgDialog()
                                Toast.makeText(this@CommunityCreateActivity, "Community created failed try again.", Toast.LENGTH_SHORT).show()
                            }

                        }
                    }
                    //dismissProgDialog()
                } else {
                    dismissProgDialog()
                    Toast.makeText(this@CommunityCreateActivity, "Error creating community", Toast.LENGTH_SHORT).show()
                }
            }
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
                        Toast.makeText(this@CommunityCreateActivity, "Importing...", Toast.LENGTH_SHORT).show()
                    }
                    val readExcelNew: List<Map<Int, Any>> = ExcelUtil.readExcelNew(this@CommunityCreateActivity, uri, uri.path)
                    Log.i(TAG, "onActivityResult:readExcelNew: ${ readExcelNew.size} ")
                    if (readExcelNew.isNotEmpty()) {
                        Log.i(TAG, "run: successfully imported")
                        runOnUiThread {
                            Toast.makeText(this@CommunityCreateActivity, "successfully imported", Toast.LENGTH_SHORT).show()
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
                            onAddMember(usersFromFileList)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@CommunityCreateActivity, "No data available", Toast.LENGTH_SHORT).show()
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