package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.NotificationRequest
import com.siffmember.info.data.remote.model.functions.NotificationResponse
import com.siffmember.info.databinding.ActivityCommunityPostsBinding
import com.siffmember.info.ui.adapter.PostMessageAdapter
import com.siffmember.info.ui.fragment.CommunityBottomSheetFragment
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.PostModel
import com.siffmember.info.ui.model.PostWithReplyCount
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.collections.isNotEmpty

class CommunityPostsActivity : BaseActivity(), CommunityBottomSheetFragment.BottomSheetListener, PostMessageAdapter.DiscussionsListener {

    companion object {
        var TAG = "CommunityPostsActivity"
    }

    private lateinit var binding: ActivityCommunityPostsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var postsViewModel: PostsMessageViewModel
    private lateinit var viewModel: CommunityViewModel
    private lateinit var postAdapter: PostMessageAdapter
    private var communityGroupId: String = ""
    private var communityGroupName: String = ""
    private lateinit var userName: String
    private lateinit var userId: String
    private var selectedUsersList = mutableListOf<MembersGroup>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCommunityPostsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeaderGcp) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // When keyboard opens -> push bottom layout up
            binding.communityList.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        db = Firebase.firestore
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        userName = sharedPref.getString(AppConstants.USER_NAME, "")!!
        userId = sharedPref.getString(AppConstants.USER_ID, "")!!
        try {
            val vBundle = intent.extras
            if (vBundle != null) {
                communityGroupId = vBundle.getString(AppConstants.COMMUNITY_GROUP_ID, null)
                communityGroupName = vBundle.getString(AppConstants.COMMUNITY_GROUP_NAME, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        binding.fabPost.setOnClickListener {
            val bottomSheetFragment = CommunityBottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }

        postsViewModel.getGroupPostsWithReplyCount(communityGroupId).observe(this) { postsWithCount ->
           try {
               val sortedPosts = postsWithCount.sortedByDescending { it.post.timestamp.toLongOrNull() ?: 0L }
               val chatMessageLists = prepareChatList(sortedPosts) // include date headers
               postAdapter = PostMessageAdapter(chatMessageLists, this, userName)

               binding.communityList.apply {
                   layoutManager = LinearLayoutManager(context).apply { stackFromEnd = false }
                   adapter = postAdapter
                   if(chatMessageLists.isNotEmpty()){
                       //Log.e(TAG,"smoothScrollToPosition")
                       smoothScrollToPosition(0)
                   }
               }
           }catch (e: Exception){
               e.printStackTrace()
           }
        }

        viewModel.getCommunitiesMemberById(communityGroupId).observe(this) { groupMember ->
            groupMember?.let {
                binding.groupNameTxt.text = groupMember.groupName
               selectedUsersList = it.members as MutableList<MembersGroup>
            } ?: run {
                // Handle case where community is null (e.g., show a message)
            }
        }

        CommunityChat.setIsChatOpen(true)
        CommunityChat.setIsReplyOpen(false)

        binding.appHeaderGcp.setOnClickListener {
            val next = Intent(this@CommunityPostsActivity, CommunityGroupEditActivity::class.java)
            next.putExtra(AppConstants.COMMUNITY_GROUP_ID, communityGroupId)
            next.putExtra(AppConstants.COMMUNITY_GROUP_NAME, communityGroupName)
            startActivity(next)
        }
    }

    fun prepareChatList(posts: List<PostWithReplyCount>): List<PostMessageAdapter.ChatItem> {
        val groupedMessages = mutableListOf<PostMessageAdapter.ChatItem>()
        var lastDate = ""
        for ((post, replyCount) in posts) {
            val formattedDate = formatDateHeader(post.timestamp.toLong())
            if (formattedDate != lastDate) {
                groupedMessages.add(PostMessageAdapter.ChatItem.DateHeader(formattedDate))
                lastDate = formattedDate
            }
            groupedMessages.add(PostMessageAdapter.ChatItem.Message(post, replyCount))
        }
        return groupedMessages
    }

    fun formatDateHeader(timestamp: Long): String {
        val now = Calendar.getInstance()
        val messageTime = Calendar.getInstance().apply { timeInMillis = timestamp }

        return when {
            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) -> "Today"

            now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) &&
                    now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 -> "Yesterday"

            else -> SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }

    override fun onAddAnnouncement(title: String, description: String) {
        showProgDialog()
        val senderID = sharedPref.getString(AppConstants.USER_ID, null)
        val senderName = sharedPref.getString(AppConstants.USER_NAME, null)
        val timestamp = System.currentTimeMillis().toString()
        if (title.isNotEmpty()) {
            postMessage(title, communityGroupName, communityGroupId, description, timestamp, senderName!!, senderID!!) { isPosted, postId ->
                if(isPosted){
                    val newMemberTokens = selectedUsersList
                        .filter { it.id != userId && it.fcmToken.isNotEmpty() }
                        .map { it.fcmToken }
                        .distinct()
                    postsViewModel.insertPostMessage(postId, title, communityGroupName, communityGroupId, description, timestamp,
                        senderName,
                        senderID
                    )
                  //  updateUserSyncPostStatus(postId, communityGroupId, senderID)
                    sendNotification(communityGroupName, description, tokens = newMemberTokens, title, postId, description, timestamp)

                }
                dismissProgDialog()
            }
        }
    }

    private fun postMessage(postTitle: String, groupName: String, groupId: String, content: String, timeStamp: String, userName: String, userId: String, onComplete: (Boolean, String) -> Unit){
        try{
            val groupDocRef = db.collection(AppConstants.TABLE_ALL_GROUPS_POST_DETAILS).document(groupId)
            groupDocRef.set(mapOf("placeholder" to true), SetOptions.merge())
            val userRef = groupDocRef.collection(AppConstants.TABLE_POST).document()
            val postData = PostModel(userRef.id, postTitle, content, timeStamp, groupName, groupId, userName, userId)
            userRef.set(postData)
                .addOnSuccessListener {
                    Log.e(TAG, "DocumentSnapshot successfully written!")
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

    private fun sendNotification(groupTitle: String, body: String, tokens: List<String>, postTitle: String, postId: String, postContent: String, timestamp: String) {
        val senderID = sharedPref.getString(AppConstants.USER_ID, null)
        val senderName = sharedPref.getString(AppConstants.USER_NAME, null)
        val notificationRequest = NotificationRequest(
            //topic = topic,
            tokens = tokens,
            title = groupTitle,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to "NewPost",
                "groupId" to communityGroupId,
                "groupName" to groupTitle,
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

    override fun onClickNext(getDiscussion: PostMessage) {
        val next = Intent(this@CommunityPostsActivity, CommunityReplyPostActivity::class.java)
        next.putExtra(AppConstants.COMMUNITY_POST_ID, getDiscussion.postId)
        next.putExtra(AppConstants.COMMUNITY_POST_NAME, getDiscussion.postTitle)
        next.putExtra(AppConstants.COMMUNITY_GROUP_ID, communityGroupId)
        next.putExtra(AppConstants.COMMUNITY_GROUP_NAME, communityGroupName)
        startActivity(next)
    }
    @OptIn(DelicateCoroutinesApi::class)
    override fun onResume() {
        super.onResume()
        CommunityChat.setIsChatOpen(true)
        CommunityChat.setIsReplyOpen(false)
        GlobalScope.launch(Dispatchers.IO) {
            val community = viewModel.getCommunitiesById(communityGroupId)
            if(community == null) {
                finish()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        CommunityChat.setIsChatOpen(false)
    }

    override fun onStop() {
        super.onStop()
        CommunityChat.setIsChatOpen(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        CommunityChat.setIsChatOpen(false)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            CommunityChat.setIsChatOpen(false)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}