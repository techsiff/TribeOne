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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.AddMemberBottomSheetLayoutBinding
import com.siffmember.info.ui.adapter.CommunityUserAdapter
import com.siffmember.info.ui.adapter.SearchUserAdapter
import com.siffmember.info.ui.model.GetUsers
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.MembersZoomMeeting
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.MeetingUserDetails

class SearchUsersBottomSheetFragment : BottomSheetDialogFragment(), SearchUserAdapter.CommunityUserListener {

    companion object {
        var TAG = "SearchUsersBottomSheetFragment"
    }

    private lateinit var binding: AddMemberBottomSheetLayoutBinding
    private lateinit var db: FirebaseFirestore
    private var progressDialog: ProgressDialog? = null
    private var listener: BottomSheetListener? = null
    private var adminID = ""

    private var userList = mutableListOf<GetUsers>()
    private var userAdapter: SearchUserAdapter? = null

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
            Log.e(TAG, "Parent Fragment must implement BottomSheetListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AddMemberBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root
        adminID = arguments?.getString("adminId").toString()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore
        progressDialog = ProgressDialog(requireActivity())

        binding.apply {
            btnAddMember.visibility = View.GONE
            spinnerCategory.visibility = View.GONE
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

        fetchUsersWithFCM()
    }
    val getUserList = mutableListOf<GetUsers>()
    private fun filterList(query: String) {
        val filteredVideos = if (query.isEmpty()) {
            getUserList  // Reset to full list when query is empty
        } else {
            getUserList.filter {
                it.name!!.contains(query, ignoreCase = true) || it.phone_number!!.contains(query, ignoreCase = true)
            }
        }
        userAdapter!!.updateList(filteredVideos)
    }
    private fun setupAdapter(){
        try{
            if (userList.isNotEmpty()) {
                val filteredUsers = userList.filter { user ->
                            (adminID.isEmpty() || user.phone_number != adminID)
                }
                    .sortedBy { it.name }
                getUserList.addAll(filteredUsers)
            }
            Log.e(TAG,"${getUserList.size}")
            if(getUserList.isEmpty()){
                binding.usersList.visibility = View.GONE
                binding.noAddMembers.visibility = View.VISIBLE
            } else {
                binding.usersList.visibility = View.VISIBLE
                binding.noAddMembers.visibility = View.GONE
            }
            userAdapter = SearchUserAdapter(getUserList, this)
            binding.usersList.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            binding.usersList.adapter = userAdapter
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun fetchUsersWithFCM() {
        try{
            showProgDialog()
            val db = FirebaseFirestore.getInstance()
            val userDetailsRef = db.collection(AppConstants.TABLE_USER_DETAILS)
            val fcmDetailsRef = db.collection(AppConstants.TABLE_FCM_DETAILS)
            val userDetailsTask = userDetailsRef.get()
            val fcmDetailsTask = fcmDetailsRef.get()

            Tasks.whenAllSuccess<QuerySnapshot>(userDetailsTask, fcmDetailsTask)
                .addOnSuccessListener { results ->

                    val usersSnapshot = results[0] as QuerySnapshot
                    val fcmSnapshot = results[1] as QuerySnapshot
                    val usersMap = mutableMapOf<String, GetUsers>()
                    // Map users by userId
                    for (doc in usersSnapshot.documents) {
                        val user = doc.toObject(GetUsers::class.java)
                        user?.let {
                            usersMap[doc.id] = it
                        }
                    }
                    // Append FCM tokens to corresponding users
                    for (doc in fcmSnapshot.documents) {
                        val userId = doc.getString("phone_number") ?: continue
                        val fcmToken = doc.getString("fcm")
                        //Log.e(CommunityCreateActivity.Companion.TAG, "Fetched Users: $fcmToken")
                        usersMap[userId]?.let { user ->
                            user.fcmToken = fcmToken  // Assuming Users class has an 'fcmToken' property
                        }
                    }
                    // Update userList
                    userList.clear()
                    userList.addAll(usersMap.values)
                    if(userList.isEmpty()){
                        binding.usersList.visibility = View.GONE
                        binding.noAddMembers.visibility = View.VISIBLE
                    } else {
                        binding.usersList.visibility = View.VISIBLE
                        binding.noAddMembers.visibility = View.GONE
                        setupAdapter()
                    }
                    dismissProgDialog()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error fetching data", e)
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
            dismissProgDialog()
        }
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

    interface BottomSheetListener {
        fun onSelectedUser(user: GetUsers)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onUserSelectListener(users: GetUsers, isSelected: Boolean) {
        try {
            listener!!.onSelectedUser(users)
            dismiss()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
}
