package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.findNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.data.local.entity.DeleteMessages
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.databinding.ActivityCommunityMainBinding
import com.siffmember.info.ui.model.Community
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.PostCommentModel
import com.siffmember.info.ui.model.PostModel
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.forEach

class CommunityMainActivity : BaseActivity() {

    companion object {
        var TAG = "CommunityMainActivity"
    }
    private lateinit var binding: ActivityCommunityMainBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var viewModel: CommunityViewModel
    private lateinit var postsViewModel: PostsMessageViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityMainBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        db = Firebase.firestore
        try {
            val user = Firebase.auth.currentUser
            if(user != null){
                val userId = sharedPref.getString(AppConstants.USER_ID, null)
                fetchAllGroupDetails(userId!!)
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        val navController = findNavController(R.id.nav_host_fragment_activity_community_main)

        binding.navView.setupWithNavController(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val currentLabel = destination.label?.toString()
            binding.btnAddCommunity.visibility = if (currentLabel == getString(R.string.title_chats)) View.VISIBLE else View.GONE
        }

        binding.btnAddCommunity.visibility =
            if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false)) View.VISIBLE else View.GONE

        binding.btnAddCommunity.setOnClickListener {
            startActivity(Intent(this, CommunityCreateActivity::class.java))
        }
        binding.refresh.setOnClickListener {
            try {
                val user = Firebase.auth.currentUser
                if(user != null){
                    val userId = sharedPref.getString(AppConstants.USER_ID, null)
                    fetchAllGroupDetails(userId!!)
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
        }
    }

    fun fetchAllGroupDetails(phoneNumber: String){
        try{
            showProgDialog()
            fetchUserGroups(phoneNumber) { groups ->
                val allRemoteGroup = ArrayList<String>()
                if (groups.isNotEmpty()) {
                    allRemoteGroup.clear()
                    lifecycleScope.launch(Dispatchers.IO) {
                        try{
                            viewModel.insertAllCommunity(groups)
                        } catch (e: Exception){
                            e.printStackTrace()
                        }
                    }
                    groups.forEach { group ->
                        //Log.e(TAG, "Fetched Group: $group")
                        allRemoteGroup.add(group.groupID)
                        lifecycleScope.launch(Dispatchers.IO) {
                            for(members in group.members){
                                if(members.phoneNumber == phoneNumber){
                                    if(CommunityChat.getFCMToken() != members.fcmToken) {
                                        updateMemberFcmToken(
                                            group.groupID,
                                            phoneNumber,
                                            CommunityChat.getFCMToken()
                                        )
                                    }
                                }
                            }

                            fetchAllPosts(group.groupID) { postsData ->
                                if(postsData.isNotEmpty()) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try{
                                            val allIds = postsData.map { it.postId }
                                            val existingIds = postsViewModel.getAlreadyInsertedPostIds(allIds)
                                            val newPosts = postsData.filter { it.postId !in existingIds }
                                            if (newPosts.isEmpty()){
                                                Log.e(TAG," Nothing to process fetchAllPosts")
                                                return@launch
                                            }  // nothing to process
                                            postsViewModel.insertAllPosts(newPosts)
                                        }catch (e: Exception){
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                            fetchCommentsForPost(group.groupID) { comments ->
                                if(comments.isNotEmpty()) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try{
                                            val allIds = comments.map { it.commentId }
                                            val existingIds = postsViewModel.getAlreadyInsertedCommentsIds(allIds)
                                            val newPosts = comments.filter { it.commentId !in existingIds }
                                            if (newPosts.isEmpty()){
                                                Log.e(TAG," Nothing to process fetchCommentsForPost")
                                                return@launch
                                            }  // nothing to process
                                            postsViewModel.insertAllReplies(newPosts)
                                        }catch (e: Exception){
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                            fetchAllDeletePosts(group.groupID){ postsData ->
                                if(postsData.isNotEmpty()) {
                                    lifecycleScope.launch(Dispatchers.IO) {
                                        try{
                                            val allIds = postsData.map { it.postId }
                                            val existingIds = postsViewModel.getAlreadyInsertedDeletedIds(allIds)
                                            val newPosts = postsData.filter { it.postId !in existingIds }
                                            if (newPosts.isEmpty()){
                                                Log.e(TAG," Nothing to process fetchAllDeletePosts")
                                                return@launch
                                            }  // nothing to process
                                            val idsToDelete = newPosts.map { it.postId }
                                            postsViewModel.deletePostsByPostIds(idsToDelete)
                                            postsViewModel.deletePostsReplyByPostIds(idsToDelete)
                                            postsViewModel.insertDeletedMessage(newPosts)
                                        }catch (e: Exception){
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                            fetchAllDeleteComments(group.groupID){ commentsData ->
                                if(commentsData.isNotEmpty()) {
                                    try{
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            val allIds = commentsData.map { it.postId }
                                            val existingIds = postsViewModel.getAlreadyInsertedDeletedIds(allIds)
                                            val newPosts = commentsData.filter { it.postId !in existingIds }
                                            if (newPosts.isEmpty()){
                                                Log.e(TAG," Nothing to process fetchAllDeleteComments")
                                                return@launch
                                            }  // nothing to process
                                            val idsToDelete = newPosts.map { it.postId }
                                            postsViewModel.deleteReplyPostMessagesByCommentIDs(idsToDelete)
                                            postsViewModel.insertDeletedMessage(newPosts)
                                        }
                                    }catch (e: Exception){
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                    lifecycleScope.launch(Dispatchers.IO) {
                        val allLocalGroup = viewModel.getAllCommunitiesIdList()
                        Log.e(TAG, "Local group ${allLocalGroup.size} remote group ${allRemoteGroup.size}")
                        for (localId in allLocalGroup) {
                            val exists = allRemoteGroup.contains(localId)
                            viewModel.updateGroupStatus(localId, exists) // make this suspend
                        }
                    }

                } else {
                    Log.e("Firestore", "No groups found for user!")
                    lifecycleScope.launch(Dispatchers.IO) {
                        // viewModel.deleteAllCommunities()
                        val allCommunity = viewModel.getAllCommunities()
                        if (allCommunity.isNotEmpty()) {
                            allCommunity.forEach { community ->
                                viewModel.updateGroupStatus(community.groupID, false)
                            }
                        }
                    }
                }
                dismissProgDialog()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    @Suppress("UNCHECKED_CAST")
    fun updateMemberFcmToken(groupId: String, userId: String, newFcmToken: String) {
        val groupDoc = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS).document(groupId)
        groupDoc.get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    val updatedMembers = (snapshot["members"] as? List<Map<String, Any>>)?.map { member ->
                        if (member["id"] == userId) {
                            member.toMutableMap().apply {
                                this["fcmToken"] = newFcmToken
                            }
                        } else {
                            member
                        }
                    }

                    groupDoc.update("members", updatedMembers)
                        .addOnSuccessListener {
                            Log.d("Firestore", "Member FCM token updated successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Firestore", "Error updating member", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching group doc", e)
            }
    }

    fun fetchAllPosts(groupId: String, onResult: (List<PostMessage>) -> Unit) {
        val postsRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId).collection(
            AppConstants.TABLE_POST)
        postsRef.orderBy("timestamp").get()
            .addOnSuccessListener { snapshot ->
                val result = snapshot.documents.mapNotNull { doc ->
                    try {
                        // Read Firestore data safely
                        val model = doc.toObject(PostModel::class.java) ?: return@mapNotNull null

                        PostMessage(
                            postId = doc.id,          // document ID = postId
                            postTitle = model.postTitle?: "",
                            content = model.content?: "",
                            timestamp = model.timestamp?: "",
                            groupName = model.groupName?: "",
                            groupId = groupId,        // known from parameter
                            userName = model.userName?: "",
                            userId = model.userId?: ""
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }
                onResult(result)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun fetchCommentsForPost(groupId: String, onResult: (List<ReplyPostMessage>) -> Unit) {
        val commentsRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId).collection(
            AppConstants.TABLE_COMMENTS)

        commentsRef.orderBy("timestamp").get()
            .addOnSuccessListener { snapshot ->
                val comments = snapshot.documents.mapNotNull { doc ->
                    try {
                        val model = doc.toObject(PostCommentModel::class.java) ?: return@mapNotNull null

                        ReplyPostMessage(
                            0,
                            commentId = doc.id,
                            postId = model.postId ?: "",
                            postTitle = model.postTitle ?: "",
                            content = model.content ?: "",
                            timestamp = model.timestamp?: "0",
                            groupId = groupId,
                            userName = model.userName ?: "",
                            userId = model.userId ?: ""
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }

                onResult(comments)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun fetchAllDeletePosts(groupId: String, onResult: (List<DeleteMessages>) -> Unit) {
        db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DELETE_DETAILS)
            .document(groupId)
            .collection(AppConstants.TABLE_POST)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                //val postIds = snapshot.documents.map { it.id }
                val list = snapshot.documents.mapNotNull { doc ->
                    val timestamp = doc.getString("timestamp") ?: "0"
                    DeleteMessages(
                        postId = doc.id,
                        timestamp = timestamp
                    )
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    fun fetchAllDeleteComments(groupId: String, onResult: (List<DeleteMessages>) -> Unit) {
        db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DELETE_DETAILS)
            .document(groupId)
            .collection(AppConstants.TABLE_COMMENTS)
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { snapshot ->
                //val postIds = snapshot.documents.map { it.id }
                val list = snapshot.documents.mapNotNull { doc ->
                    val timestamp = doc.getString("timestamp") ?: "0"
                    DeleteMessages(
                        postId = doc.id,
                        timestamp = timestamp
                    )
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun fetchUserGroups(userId: String, onGroupsFetched: (List<CommunityEntity>) -> Unit) {
        val userGroupRef = db.collection(AppConstants.TABLE_USER_GROUPS_DETAILS).document(userId)
        userGroupRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get the groupsId array safely
                    val groupsIdList = document.get("groupsId") as? List<String> ?: emptyList()
                    //Log.e("Firestore", "User's groups: $groupsIdList")
                    if (groupsIdList.isNotEmpty()) {
                        val fetchedGroups = mutableListOf<CommunityEntity>()
                        val pendingRequests = AtomicInteger(groupsIdList.size) // Track pending API calls

                        groupsIdList.forEach { groupId ->
                            fetchGroupDetails(groupId) { groupData ->
                                groupData?.let {
                                    fetchedGroups.add(CommunityEntity(it.id, it.name, it.description, it.createdBy, it.createdAt, it.groupIcon, true, it.members))
                                }
                                // When all groups are fetched, call onGroupsFetched
                                if (pendingRequests.decrementAndGet() == 0) {
                                    onGroupsFetched(fetchedGroups)
                                }
                            }
                        }
                    } else {
                        Log.e("Firestore", "User is not part of any group")
                        onGroupsFetched(emptyList()) // Return empty list if no groups
                    }
                } else {
                    Log.e("Firestore", "No document found for user: $userId")
                    onGroupsFetched(emptyList())
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching user groups", e)
                onGroupsFetched(emptyList()) // Return empty list on failure
            }
    }

    @Suppress("UNCHECKED_CAST")
    fun fetchGroupDetails(groupId: String, onGroupFetched: (Community?) -> Unit) {
        val groupRef = db.collection(AppConstants.TABLE_ALL_GROUPS_DETAILS)
            .document(groupId)
        groupRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val groupData = document.data
                    // Log.e("Firestore", "Group Details: $groupData")
                    if (groupData != null) {
                        try {
                            // Convert the members list safely
                            val membersList = (groupData["members"] as? List<Map<String, Any>>)?.map { memberMap ->
                                MembersGroup(
                                    id = memberMap["id"] as? String ?: "",
                                    name = memberMap["name"] as? String ?: "",
                                    phoneNumber = memberMap["phoneNumber"] as? String ?: "",
                                    isAdmin = memberMap["admin"] as? Boolean == true,
                                    fcmToken = memberMap["fcmToken"] as? String ?: ""
                                )
                            } ?: emptyList()

                            val community = Community(
                                id = document.id,
                                name = groupData["name"] as? String ?: "",
                                description = groupData["description"] as? String ?: "",
                                createdBy = groupData["createdBy"] as? String ?: "",
                                createdAt = groupData["createdAt"] as? String ?: "",
                                groupIcon = groupData["groupIcon"] as? String ?: "",
                                members = membersList
                            )
                            //Log.e("Firestore", "Fetched Group: $community")
                            onGroupFetched(community)

                        } catch (e: Exception) {
                            Log.e("Firestore", "Error parsing group data", e)
                            onGroupFetched(null)
                        }
                    } else {
                        Log.e("Firestore", "Group data is null for ID: $groupId")
                        onGroupFetched(null)
                    }
                } else {
                    Log.e("Firestore", "No group found with ID: $groupId")
                    onGroupFetched(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching group details", e)
                onGroupFetched(null)
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