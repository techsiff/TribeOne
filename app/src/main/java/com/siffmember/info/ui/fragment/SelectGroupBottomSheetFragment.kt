package com.siffmember.info.ui.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.data.local.entity.DeleteMessages
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.databinding.NewMeetingBottomSheetLayoutBinding
import com.siffmember.info.ui.activity.CommunityMainActivity
import com.siffmember.info.ui.adapter.MeetingGroupSelectAdapter
import com.siffmember.info.ui.model.Community
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.PostCommentModel
import com.siffmember.info.ui.model.PostModel
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

class SelectGroupBottomSheetFragment : BottomSheetDialogFragment(), MeetingGroupSelectAdapter.GroupSelectedListener {

    companion object {
        var TAG = "SelectGroupBottomSheetFragment"
    }

    private lateinit var binding: NewMeetingBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null
    private lateinit var viewModel: CommunityViewModel
    private lateinit var postsViewModel: PostsMessageViewModel

    private var groupList = mutableListOf<CommunityEntity>()
    private var groupsAdapter: MeetingGroupSelectAdapter? = null
    private var selectedUsersList = mutableListOf<MembersGroup>()
    private var blockedUserIds  = mutableListOf<String>()
    private lateinit var db: FirebaseFirestore
    private var progressDialog: ProgressDialog? = null
    private var adminID = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true
                it.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
            }
        }

        return dialog
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.layoutParams?.height =
            ViewGroup.LayoutParams.MATCH_PARENT
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the parent activity implements the interface
        if (context is BottomSheetListener) {
            listener = context
        } else {
            Log.e(TAG, "Parent activity must implement BottomSheetListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NewMeetingBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root
        adminID = arguments?.getString("adminId").toString()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]

        db = Firebase.firestore
        progressDialog = ProgressDialog(requireActivity())
        try{
            lifecycleScope.launch {
                groupList.clear()
                val allCommunity = withContext(Dispatchers.IO) {
                    viewModel.getAllCommunities()
                }
                if (allCommunity.isNotEmpty()) {
                    groupList = allCommunity as MutableList<CommunityEntity>
                } else {
                    fetchAllGroupDetails(adminID)
                }
                fetchBlockedUsers{ users ->
                    if(users.isNotEmpty()) {
                        blockedUserIds = users as MutableList<String>
                    }
                    dismissProgDialog()
                }
                setupAdapter()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }

        binding.apply {
            btnAddGroup.visibility = View.GONE
            btnAddGroup.setOnClickListener {
                try {
                    Log.e(TAG,"selected users::: ${selectedUsersList.size}")
                    listener!!.onAddMember(selectedUsersList)
                    dismiss()
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            userSearchEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    //
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterList(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {
                    //
                }
            })
        }
    }
    private fun filterList(query: String) {
        val filteredVideos = if (query.isEmpty()) {
            groupList  // Reset to full list when query is empty
        } else {
            groupList.filter {
                it.groupName.contains(query, ignoreCase = true)
            }
        }
        groupsAdapter!!.updateList(filteredVideos)
    }

    private fun setupAdapter(){
        try{
            if(groupList.isEmpty()){
                binding.usersList.visibility = View.GONE
                binding.noAddMembers.visibility = View.VISIBLE
            } else {
                binding.usersList.visibility = View.VISIBLE
                binding.noAddMembers.visibility = View.GONE
            }
            groupsAdapter = MeetingGroupSelectAdapter(groupList, this)
            binding.usersList.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            binding.usersList.adapter = groupsAdapter
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    @Suppress("UNCHECKED_CAST")
    fun fetchBlockedUsers(onResult: (List<String>) -> Unit){
        try{
            db.collection(AppConstants.TABLE_USER_BLOCK)
                .get()
                .addOnSuccessListener { snapshot ->
                    val blockedUser = mutableSetOf<String>() // avoid duplicates
                    snapshot.documents.forEach { doc ->
                        val users = doc.get("blockedUsers") as? List<String>
                        users?.let { blockedUser.addAll(it) }
                    }
                    Log.e(TAG, "Total blocked users: ${blockedUser.size}")
                    onResult(blockedUser.toList())
                }
                .addOnFailureListener {
                    onResult(emptyList())
                }

        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    interface BottomSheetListener {
        fun onAddMember(selectedUsersList: List<MembersGroup>)
    }

    /**
     * Showing progress dialog
     */
    fun showProgDialog() {
        try {
            progressDialog!!.setMode(ProgressDialog.MODE_INDETERMINATE)
            progressDialog!!.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dismiss progress dialog
     */
    fun dismissProgDialog() {
        try{
            if(progressDialog != null){
                progressDialog!!.dismiss()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onGroupSelectListener(groups: CommunityEntity, isSelected: Boolean) {

        groups.members.forEach { user ->
            if(!blockedUserIds.contains(user.phoneNumber)){
                if(adminID != user.phoneNumber) {
                    val member = MembersGroup(
                        id = user.phoneNumber,
                        name = user.name,
                        phoneNumber = user.phoneNumber,
                        fcmToken = user.fcmToken
                    )
                    if (isSelected) {
                        if (selectedUsersList.none { it.phoneNumber == user.phoneNumber }) {
                            selectedUsersList.add(member)
                        }
                    } else {
                        selectedUsersList.removeAll {
                            it.phoneNumber == user.phoneNumber
                        }
                    }
                }
            }
        }
        //Log.e(TAG, "selected users::: ${selectedUsersList.size}")
        binding.btnAddGroup.visibility =
            if (selectedUsersList.isNotEmpty()) View.VISIBLE else View.GONE
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
                    lifecycleScope.launch(Dispatchers.IO) {
                        val getGroupList = viewModel.getAllCommunities()
                        Log.e(TAG, "Local getGroupList ${getGroupList.size}")
                        if (getGroupList.isNotEmpty()) {
                            lifecycleScope.launch(Dispatchers.Main) {
                                groupList.clear()
                                groupList = getGroupList as MutableList<CommunityEntity>
                                setupAdapter()
                            }

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
}
