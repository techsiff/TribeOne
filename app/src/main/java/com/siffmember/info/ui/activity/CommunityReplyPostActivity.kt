package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.TextWatcher
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.NotificationRequest
import com.siffmember.info.data.remote.model.functions.NotificationResponse
import com.siffmember.info.databinding.ActivityCommunityReplyBinding
import com.siffmember.info.ui.adapter.ReplyPostMessageAdapter
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.PostCommentModel
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat
import com.siffmember.info.utils.Utils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class CommunityReplyPostActivity : BaseActivity(), ReplyPostMessageAdapter.ReplyPostListener {

    companion object {
        var TAG = "CommunityReplyPostActivity"
    }

    private lateinit var binding: ActivityCommunityReplyBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var postsViewModel: PostsMessageViewModel
    private lateinit var viewModel: CommunityViewModel

    private var recyclerViewAdapter: ReplyPostMessageAdapter? = null
    private var replyList: ArrayList<ReplyPostMessage> = ArrayList()
    private val selectedReplies = mutableSetOf<ReplyPostMessage>()
    private var postId: String = ""
    private var postTitle: String = ""
    private var groupName: String = ""
    private var groupId: String = ""
    private var postContent: String = ""
    private lateinit var userName: String
    private lateinit var userID: String
    private var selectedUsersList = mutableListOf<MembersGroup>()
    private var userIsAtBottom = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityReplyBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // When keyboard opens -> push bottom layout up
            binding.replyBtnLl.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        db = Firebase.firestore
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]

        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                postId = vBundle.getString(AppConstants.COMMUNITY_POST_ID, null)
                groupId = vBundle.getString(AppConstants.COMMUNITY_GROUP_ID, null)
                groupName = vBundle.getString(AppConstants.COMMUNITY_GROUP_NAME, null)
            }
            userName = sharedPref.getString(AppConstants.USER_NAME, "")!!
            userID = sharedPref.getString(AppConstants.USER_ID, "")!!
        } catch (e: Exception) {
            e.printStackTrace()
        }
        CommunityChat.setIsReplyOpen(true)
        CommunityChat.setIsChatOpen(false)
        binding.apply {
            binding.deletePost.visibility =
                if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false)) View.VISIBLE else View.GONE

            deleteComments.visibility = View.GONE
            btnPost.setOnClickListener {
                val content = binding.communityEdit.text.toString()
                sendPost(content)
            }

            deletePost.setOnClickListener {
                deletePostDialog()
            }

            deleteComments.setOnClickListener {
                if(selectedReplies.isNotEmpty()){
                    showProgDialog()
                    val commentsIds = mutableListOf<String>()
                    val total = selectedReplies.size
                    var completed = 0
                    val timestamp = System.currentTimeMillis().toString()
                    selectedReplies.forEach { postReplyMessage ->
                        deleteComments(postReplyMessage.commentId, groupId){ isDeleted ->
                            if(isDeleted) {
                                commentsIds.add(postReplyMessage.commentId)
                                updateCommentsDeleteStatus(postReplyMessage.commentId, groupId, timestamp)
                                postsViewModel.deleteReplyPostMessagesByID(postReplyMessage.id.toString())
                            }
                            completed++
                            if (completed == total) {
                                val commentId = commentsIds.joinToString(",")
                                val newMemberTokens = selectedUsersList
                                    .filter { it.id != userID && it.fcmToken.isNotEmpty() }
                                    .map { it.fcmToken }
                                    .distinct()
                                sendNotificationDeleteComments(
                                    groupName,
                                    "$userName deleted comments from this $postTitle",
                                    newMemberTokens,
                                    commentId,
                                    timestamp,
                                )
                                dismissProgDialog()
                            }
                        }
                    }
                }
            }

            communityEdit.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    collapseContent()
                }
            }

            communityEdit.setOnClickListener {
                collapseContent()
            }

            communityEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    //
                }

                @SuppressLint("SetTextI18n")
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val length = s?.length ?: 0
                    charCounter.text = "$length/550"
                    // Optional: turn red when limit reached
                    if (length >= 550) {
                        charCounter.setTextColor(ContextCompat.getColor(this@CommunityReplyPostActivity, R.color.red))
                    } else {
                        charCounter.setTextColor(ContextCompat.getColor(this@CommunityReplyPostActivity, R.color.grey))
                    }
                }

                override fun afterTextChanged(s: Editable?) {
                    //
                }
            })
        }

        postsViewModel.getGroupPostReplyMessages(postId).observe(this) { replyPost ->
            replyList.clear()
            val sortedAnnouncements = sortAnnouncementsByDate(replyPost)
            sortedAnnouncements.forEach {
                replyList.add(it)
            }
            setupAdapter()
        }
        postsViewModel.getLatestPostMessageByPostId(postId).observe(this) { post ->
            if(post != null) {
                binding.titleTextView.text = post.postTitle
                postContent = post.content
                postTitle = post.postTitle
                collapseContent()
            }
        }
        viewModel.getCommunitiesMemberById(groupId).observe(this) { groupMember ->
            groupMember?.let {
                selectedUsersList = it.members as MutableList<MembersGroup>
            } ?: run {
                // Handle case where community is null (e.g., show a message)
            }
        }
        binding.communityList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {

                val lm = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = lm.findFirstVisibleItemPosition() // reverseLayout -> bottom = index 0

                // If bottom item (0) is visible -> user is at bottom
                userIsAtBottom = lastVisible == 0
            }
        })
    }

    private fun collapseContent() {
        binding.contentTextView.makeExpandable(
            fullText = postContent,   // store this when loading the post
            maxLines = 4,
            expandText = " Show More",
            collapseText = " Show Less",
            linkColor = "#EE7103".toColorInt()
        )
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true

        binding.communityList.layoutManager = layoutManager
        recyclerViewAdapter = ReplyPostMessageAdapter(replyList, userName, this,  sharedPref.getBoolean(AppConstants.IS_ADMIN, false))
        binding.communityList.adapter = recyclerViewAdapter
        binding.replyCountTotal.text = Utils.formatDynamicCount(replyList.size.toLong())

        binding.communityList.post {
            binding.communityList.scrollToPosition(0)
        }

        // Auto-scroll when keyboard opens
        binding.communityList.viewTreeObserver.addOnGlobalLayoutListener {
            val heightDiff = binding.root.rootView.height - binding.root.height
            if (heightDiff > 250) {  // keyboard opened
                binding.communityList.post {
                    binding.communityList.scrollToPosition(0)
                }
            }
        }
    }

    fun TextView.makeExpandable(
        fullText: String,
        maxLines: Int = 4,
        expandText: String = " Show More",
        collapseText: String = " Show Less",
        linkColor: Int = Color.BLUE
    ) {
        this.text = fullText

        this.post {
            if (this.layout == null) return@post
            if (lineCount <= maxLines) return@post

            // Get truncated text safely
            val endIndex = this.layout.getLineEnd(maxLines - 1)
            val visibleText = fullText.take(endIndex).trim()

            setExpandableText(
                visibleText,
                expandText,
                fullText,
                collapseText,
                maxLines,
                linkColor
            )
        }
    }

    private fun TextView.setExpandableText(
        visibleText: String,
        expandText: String,
        fullText: String,
        collapseText: String,
        maxLines: Int,
        linkColor: Int
    ) {
        val spannable = SpannableStringBuilder("$visibleText...$expandText")

        val start = spannable.indexOf(expandText)
        val end = start + expandText.length

        val expandSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                setCollapseText(fullText, collapseText, maxLines, expandText, linkColor)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = linkColor
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(expandSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        this.text = spannable
        this.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun TextView.setCollapseText(
        fullText: String,
        collapseText: String,
        maxLines: Int,
        expandText: String,
        linkColor: Int
    ) {
        val spannable = SpannableStringBuilder("$fullText\n$collapseText")

        val start = spannable.indexOf(collapseText)
        val end = start + collapseText.length

        val collapseSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                makeExpandable(fullText, maxLines, expandText, collapseText, linkColor)
            }

            override fun updateDrawState(ds: TextPaint) {
                ds.color = linkColor
                ds.isUnderlineText = false
            }
        }

        spannable.setSpan(collapseSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        this.text = spannable
        this.movementMethod = LinkMovementMethod.getInstance()
    }

    private fun sendPost(content: String){
        try {
            val senderID = sharedPref.getString(AppConstants.USER_ID, null)
            val senderName = sharedPref.getString(AppConstants.USER_NAME, null)
            val timestamp = System.currentTimeMillis().toString()
            if (content.isNotEmpty()) {
                showProgDialog()
                postComments(postTitle, groupId, content, timestamp, senderName!!, senderID!!) { isPosted, commentId ->
                    if(isPosted){
                        postsViewModel.sendPostReplyMessage(
                            commentId,
                            postId,
                            postTitle,
                            content,
                            timestamp,
                            groupId,
                            senderName,
                            senderID
                        )
                        //updateUserSyncCommentStatus(commentId, groupId, senderID)
                        binding.communityEdit.text!!.clear()
                        val newMemberTokens = selectedUsersList
                            .filter { it.id != userID && it.fcmToken.isNotEmpty() }
                            .map { it.fcmToken }
                            .distinct()
                        sendNotification(groupName, content, tokens = newMemberTokens, commentId, content, timestamp)
                    }
                    dismissProgDialog()
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun postComments(postTitle: String, groupId: String, content: String, timeStamp: String, userName: String, userId: String, onComplete: (Boolean, String) -> Unit){
        try{
            val groupDocRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId)
            groupDocRef.set(mapOf("placeholder" to true), SetOptions.merge())
            val userRef = groupDocRef.collection(AppConstants.TABLE_COMMENTS).document()
            val postData = PostCommentModel(userRef.id, postId ,postTitle, content, timeStamp, userName, userId)
            userRef.set(postData)
                .addOnSuccessListener {
                    Log.e(TAG, "DocumentSnapshot successfully written!")
                    onComplete(true, userRef.id)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    onComplete(false, userRef.id)
                }
        } catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    fun updateCommentsDeleteStatus(commentId: String, groupId: String, timestamp: String) {
        val groupDocRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DELETE_DETAILS).document(groupId)
        groupDocRef.set(mapOf("timestamp" to timestamp), SetOptions.merge())
        val userRef = groupDocRef.collection(AppConstants.TABLE_COMMENTS).document(commentId)
        val updateMap = mapOf("timestamp" to timestamp)
        userRef.set(updateMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.e("Sync", "synced comments delete $postId")
            }
            .addOnFailureListener { e ->
                Log.e("Sync", "Error syncing comments delete: $e")
            }
    }

    private fun deleteComments(commentId: String, groupId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId).collection(AppConstants.TABLE_COMMENTS).document(commentId)
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
    fun updatePostDeleteStatus(postId: String, groupId: String, timestamp: String) {

        val groupDocRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DELETE_DETAILS).document(groupId)
        groupDocRef.set(mapOf("timestamp" to timestamp), SetOptions.merge())
        val userRef = groupDocRef.collection(AppConstants.TABLE_POST).document(postId)
        val updateMap = mapOf("timestamp" to timestamp)
        userRef.set(updateMap, SetOptions.merge())
            .addOnSuccessListener {
                Log.e("Sync", "synced post delete $postId")
            }
            .addOnFailureListener { e ->
                Log.e("Sync", "Error syncing post delete: $e")
            }
    }

    private fun deletePost(postId: String, groupId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId).collection(AppConstants.TABLE_POST).document(postId)
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

    private fun deletePostDialog(){
        try{
            android.app.AlertDialog.Builder(this)
                .setTitle("Delete Post Alert")
                .setMessage("Are you sure you want to delete this post?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    showProgDialog()
                    deletePost(postId, groupId) { isDeleted ->
                        if (isDeleted) {
                            val timestamp = System.currentTimeMillis().toString()
                            val newMemberTokens = selectedUsersList
                                .filter { it.id != userID && it.fcmToken.isNotEmpty() }
                                .map { it.fcmToken }
                                .distinct()
                            sendNotificationDeletePost(
                                groupName,
                                "$userName deleted this post $postTitle from $groupName",
                                newMemberTokens,
                                postId,
                                timestamp,
                            )
                            updatePostDeleteStatus(postId, groupId, timestamp)
                            postsViewModel.deletePostMessagesByPostId(postId)
                            postsViewModel.deleteReplyPostMessagesByPostID(postId)
                            finish()
                        }
                        dialogInterface.dismiss()
                    }
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

    private fun sortAnnouncementsByDate(getDiscussions: List<ReplyPostMessage>): List<ReplyPostMessage> {
        return getDiscussions.sortedByDescending { getDiscussion ->
            getDiscussion.timestamp
        }
    }

    private fun sendNotification(groupTitle: String, body: String, tokens: List<String>, commentId: String, content: String, timestamp: String) {
        val senderID = sharedPref.getString(AppConstants.USER_ID, null)
        val senderName = sharedPref.getString(AppConstants.USER_NAME, null)
        val notificationRequest = NotificationRequest(
            //topic = topic,
            tokens = tokens,
            title = groupTitle,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to "NewReplyPost",
                "senderId" to senderID.toString(),
                "senderName" to senderName.toString(),
                "groupId" to groupId,
                "groupName" to groupTitle,
                "commentId" to commentId,
                "postTitle" to postTitle,
                "postId" to postId,
                "commentContent" to content,
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

    override fun onResume() {
        super.onResume()
        CommunityChat.setIsReplyOpen(true)
        CommunityChat.setIsChatOpen(false)
    }

    override fun onPause() {
        super.onPause()
        CommunityChat.setIsReplyOpen(false)
    }

    override fun onStop() {
        super.onStop()
        CommunityChat.setIsReplyOpen(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        CommunityChat.setIsReplyOpen(false)
    }

    override fun onClickNext(replyPostMessage: ReplyPostMessage) {
        //
    }

    override fun onSelectionChanged(selected: List<ReplyPostMessage>) {
        selectedReplies.addAll(selected)
        if(selected.isEmpty()){
            binding.deleteComments.visibility = View.GONE
        } else {
            binding.deleteComments.visibility = View.VISIBLE
        }
    }
    private fun sendNotificationDeletePost(title: String, body: String, tokens: List<String>, postId: String, timestamp: String) {
        val notificationRequest = NotificationRequest(
            tokens = tokens,  // Replace with the actual FCM token
            title = title,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to "DeletePost",
                "groupId" to groupId,
                "groupName" to groupName,
                "senderId" to "",
                "senderName" to "",
                "postTitle" to "",
                "postId" to postId,
                "postContent" to "",
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
    private fun sendNotificationDeleteComments(title: String, body: String, tokens: List<String>, commentId: String, timestamp: String) {
        val notificationRequest = NotificationRequest(
            tokens = tokens,  // Replace with the actual FCM token
            title = title,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to "DeleteComments",
                "groupId" to groupId,
                "groupName" to groupName,
                "senderId" to "",
                "senderName" to "",
                "postTitle" to "",
                "commentId" to commentId,
                "postContent" to "",
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
            CommunityChat.setIsReplyOpen(false)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}