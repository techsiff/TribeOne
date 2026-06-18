package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.NewMeetingBottomSheetLayoutBinding
import com.siffmember.info.ui.adapter.MeetingTaggedUsersSelectAdapter
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.TaggedUsers
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.utils.AppConstants
import kotlinx.coroutines.launch

class SelectTaggedUsersBottomSheetFragment : BottomSheetDialogFragment(), MeetingTaggedUsersSelectAdapter.GroupSelectedListener {

    companion object {
        var TAG = "SelectTaggedUsersBottomSheetFragment"
    }

    private lateinit var binding: NewMeetingBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null

    private var taggedList = mutableListOf<TaggedUsers>()
    private var groupsAdapter: MeetingTaggedUsersSelectAdapter? = null
    private var selectedUsersList = mutableListOf<MembersGroup>()
    private var blockedUserIds  = mutableListOf<String>()
    private lateinit var db: FirebaseFirestore
    private var progressDialog: ProgressDialog? = null
    private var adminID = ""

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<View>(R.id.design_bottom_sheet)
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
        dialog?.findViewById<View>(R.id.design_bottom_sheet)?.layoutParams?.height =
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

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = NewMeetingBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root
        adminID = arguments?.getString("adminId").toString()
        binding.addMemberTitle.text = "Select Tags"
        binding.userSearchEdit.setHint("Search tag name")
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore
        progressDialog = ProgressDialog(requireActivity())
        try{
            lifecycleScope.launch {
                taggedList.clear()
                fetchTaggedUsers()
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
            taggedList  // Reset to full list when query is empty
        } else {
            taggedList.filter {
                it.title.contains(query, ignoreCase = true)
            }
        }
        groupsAdapter!!.updateList(filteredVideos)
    }

    private fun setupAdapter(){
        try{
            if(taggedList.isEmpty()){
                binding.usersList.visibility = View.GONE
                binding.noAddMembers.visibility = View.VISIBLE
            } else {
                binding.usersList.visibility = View.VISIBLE
                binding.noAddMembers.visibility = View.GONE
            }
            groupsAdapter = MeetingTaggedUsersSelectAdapter(taggedList, this)
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

    @Suppress("UNCHECKED_CAST")
    private fun fetchTaggedUsers() {
        try {
            showProgDialog()
            db.collection(AppConstants.TABLE_USER_TAGS)
                .get()
                .addOnSuccessListener { snapshot ->
                    val result = mutableListOf<TaggedUsers>()
                    for (doc in snapshot.documents) {
                        val rawList = doc.get("taggedUsers") as? List<HashMap<String, Any>> ?: emptyList()
                        val users = rawList.map {
                            MembersGroup(
                                id = it["id"] as? String ?: "",
                                name = it["name"] as? String ?: "",
                                phoneNumber = it["phoneNumber"] as? String ?: "",
                                fcmToken = it["fcmToken"] as? String ?: ""
                            )
                        }
                        result.add(TaggedUsers(doc.id, users))
                    }
                    taggedList.addAll(result)
                    Log.e(TAG, "Tagged users count: ${result.size}")
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

    override fun onGroupSelectListener(groups: TaggedUsers, isSelected: Boolean) {
        groups.users.forEach { user ->
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


}
