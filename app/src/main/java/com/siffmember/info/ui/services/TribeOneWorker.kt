package com.siffmember.info.ui.services

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.data.local.database.AppDatabase
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.data.local.entity.DeleteMessages
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.data.local.repository.CommunityRepository
import com.siffmember.info.data.local.repository.PostMessageRepository
import com.siffmember.info.ui.model.Community
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.PostCommentModel
import com.siffmember.info.ui.model.PostModel
import com.siffmember.info.utils.AppConstants
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TribeOneWorker(val context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

     companion object{
         const val TAG = "TribeOneWorker"
     }
    private lateinit var repository: CommunityRepository
    private lateinit var postRepository: PostMessageRepository
    lateinit var sharedPref: SharedPreferences
    private lateinit var db: FirebaseFirestore

    @OptIn(DelicateCoroutinesApi::class)
    override fun doWork(): Result {
        db = Firebase.firestore
        val communityDao = AppDatabase.getDatabase(context).communityDao()
        repository = CommunityRepository(communityDao)
        postRepository = PostMessageRepository(context)
        sharedPref = context.getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)

        val notificationType = inputData.getString("notificationType")
        val groupId = inputData.getString("groupId")
        val groupName = inputData.getString("groupName")
        val senderName = inputData.getString("senderName")
        val senderId = inputData.getString("senderId")
        val postId = inputData.getString("postId")
        val postTitle = inputData.getString("postTitle")
        val postContent = inputData.getString("postContent")
        val commentId = inputData.getString("commentId")
        val commentContent = inputData.getString("commentContent")
        val timestamp = inputData.getString("timestamp")

        //val phoneNumber = sharedPref.getString(AppConstants.USER_ID, "").toString()
        Log.e(TAG, "notificationType: $notificationType GroupId: $groupId postID: $postId commentID: $commentId")

        try {
            when (notificationType) {
                "NewCommunity" -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        if(groupId!!.isNotEmpty()) {

                            fetchGroupDetails(groupId) { groupData ->
                                groupData?.let {
                                    val community = CommunityEntity(
                                        it.id,
                                        it.name,
                                        it.description,
                                        it.createdBy,
                                        it.createdAt,
                                        it.groupIcon,
                                        true,
                                        it.members
                                    )
                                    repository.insertCommunity(community)

                                }
                            }

                            if(groupName!!.isNotEmpty() && postId!!.isNotEmpty() && postTitle!!.isNotEmpty() && postContent!!.isNotEmpty() && timestamp!!.isNotEmpty() && senderName!!.isNotEmpty() && senderId!!.isNotEmpty()){
                                try{
                                    postRepository.insertPostMessage(PostMessage(postId, postTitle, postContent, timestamp, groupName, groupId, senderName, senderId))
                                }catch (e: Exception){
                                    e.printStackTrace()
                                }
                            } else if(postId!!.isNotEmpty()) {
                                fetchAllPosts(groupId, postId) { postsData ->
                                    try {
                                        postRepository.insertPostMessage(postsData)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
                "NewPost" -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        if(groupId!!.isNotEmpty()) {
                            if(groupName!!.isNotEmpty() && postId!!.isNotEmpty() && postTitle!!.isNotEmpty() && postContent!!.isNotEmpty() && timestamp!!.isNotEmpty() && senderName!!.isNotEmpty() && senderId!!.isNotEmpty()){
                                try{
                                    postRepository.insertPostMessage(PostMessage(postId, postTitle, postContent, timestamp, groupName, groupId, senderName, senderId))
                                }catch (e: Exception){
                                    e.printStackTrace()
                                }
                            } else if(postId!!.isNotEmpty()) {
                                fetchAllPosts(groupId, postId) { postsData ->
                                    try {
                                        postRepository.insertPostMessage(postsData)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
                "NewReplyPost" -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        if(groupId!!.isNotEmpty()) {
                            if(postId!!.isNotEmpty() && postTitle!!.isNotEmpty() && postContent!!.isNotEmpty() && commentId!!.isNotEmpty() && commentContent!!.isNotEmpty() && timestamp!!.isNotEmpty() && senderName!!.isNotEmpty() && senderId!!.isNotEmpty()){
                                try {
                                    postRepository.insertPostReplyMessage(ReplyPostMessage(0, commentId, postId, postTitle, postContent, timestamp, groupId, senderName, senderId))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            } else if(commentId!!.isNotEmpty()) {
                                fetchCommentsForPost(groupId, commentId) { comments ->
                                    try {
                                        postRepository.insertPostReplyMessage(comments)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                    }
                }
                "DeletePost" -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        if (groupId!!.isNotEmpty()) {
                            if(postId!!.isNotEmpty() && timestamp!!.isNotEmpty()) {
                                postRepository.deletePostMessagesByPostId(postId)
                                postRepository.deleteReplyPostMessagesByPostID(postId)
                                postRepository.insertDeletedMessage(listOf(DeleteMessages(0, postId, timestamp)))
                            } else {
                                fetchAllDeletePosts(groupId) { postsData ->
                                    if (postsData.isNotEmpty()) {
                                        GlobalScope.launch(Dispatchers.IO) {
                                            try {
                                                val allIds = postsData.map { it.postId }
                                                val existingIds = postRepository.getAlreadyInsertedDeletedIds(allIds)
                                                val newPosts = postsData.filter { it.postId !in existingIds }
                                                if (newPosts.isEmpty()) {
                                                    Log.e(TAG, " Nothing to process fetchAllDeletePosts")
                                                    return@launch
                                                }  // nothing to process
                                                val idsToDelete = newPosts.map { it.postId }
                                                postRepository.deletePostsByPostIds(idsToDelete)
                                                postRepository.deletePostsReplyByPostIds(idsToDelete)
                                                postRepository.insertDeletedMessage(newPosts)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "DeleteComments" -> {
                    GlobalScope.launch(Dispatchers.IO) {
                        if (groupId!!.isNotEmpty()) {
                            if(commentId!!.isNotEmpty() && timestamp!!.isNotEmpty()) {
                                val newPosts = commentId.split(",")
                                postRepository.deleteReplyPostMessagesByCommentIDs(newPosts)
                                newPosts.forEach { commentId ->
                                    postRepository.insertDeletedMessage(listOf(DeleteMessages(0,commentId, timestamp)))
                                }
                            } else {
                                fetchAllDeleteComments(groupId) { commentsData ->
                                    if (commentsData.isNotEmpty()) {
                                        try {
                                            GlobalScope.launch(Dispatchers.IO) {
                                                val allIds = commentsData.map { it.postId }
                                                val existingIds = postRepository.getAlreadyInsertedDeletedIds(allIds)
                                                val newPosts =
                                                    commentsData.filter { it.postId !in existingIds }
                                                if (newPosts.isEmpty()) {
                                                    Log.e(TAG, " Nothing to process fetchAllDeleteComments")
                                                    return@launch
                                                }  // nothing to process
                                                val idsToDelete = newPosts.map { it.postId }
                                                postRepository.deleteReplyPostMessagesByCommentIDs(idsToDelete)
                                                postRepository.insertDeletedMessage(newPosts)
                                            }
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                }
                            }

                        }
                    }
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }


        return Result.success()
    }

    fun fetchAllPosts(groupId: String, postId: String, onResult: (PostMessage) -> Unit) {
        val postsRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId).collection(
            AppConstants.TABLE_POST).document(postId)
        postsRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onResult(PostMessage("", "", "", "", "", "", "", ""))
                    return@addOnSuccessListener
                }
                val model = snapshot.toObject(PostModel::class.java)
                if (model == null) {
                    onResult(PostMessage("", "", "", "", "", "", "", ""))
                    return@addOnSuccessListener
                }
                val postData = PostMessage(
                    postId = snapshot.id,          // document ID = postId
                    postTitle = model.postTitle?: "",
                    content = model.content?: "",
                    timestamp = model.timestamp?: "",
                    groupName = model.groupName?: "",
                    groupId = groupId,        // known from parameter
                    userName = model.userName?: "",
                    userId = model.userId?: ""
                )
                onResult(postData)
            }
            .addOnFailureListener {
                onResult(PostMessage("", "", "", "", "", "", "", ""))
            }
    }

    fun fetchCommentsForPost(groupId: String, commentId: String, onResult: (ReplyPostMessage) -> Unit) {
        val commentsRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId).collection(
            AppConstants.TABLE_COMMENTS).document(commentId)
        commentsRef.get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.exists()) {
                    onResult(ReplyPostMessage(0, "", "", "", "", "", "", "", ""))
                    return@addOnSuccessListener
                }
                val model = snapshot.toObject(PostCommentModel::class.java)
                // If model == null, respond with empty reply
                if (model == null) {
                    onResult(ReplyPostMessage(0, "", "", "", "", "", "", "", ""))
                    return@addOnSuccessListener
                }
                val replyData = ReplyPostMessage(
                    0,
                    commentId = snapshot.id,
                    postId = model.postId ?: "",
                    postTitle = model.postTitle ?: "",
                    content = model.content ?: "",
                    timestamp = model.timestamp?: "0",
                    groupId = groupId,
                    userName = model.userName ?: "",
                    userId = model.userId ?: ""
                )
                onResult(replyData)
            }
            .addOnFailureListener {
                onResult(ReplyPostMessage(0, "", "", "", "", "", "", "", ""))
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

    /*fun fetchUserGroups(userId: String, onGroupsFetched: (List<CommunityEntity>) -> Unit) {
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
    }*/

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
}