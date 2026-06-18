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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.AddGroupMemberBottomSheetLayoutBinding
import com.siffmember.info.databinding.AddMemberBottomSheetLayoutBinding
import com.siffmember.info.ui.activity.CommunityCreateActivity
import com.siffmember.info.ui.adapter.CommunityUserAdapter
import com.siffmember.info.ui.model.GetUsers
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.Utils
import kotlinx.coroutines.launch

class AddMemberBottomSheetFragment : BottomSheetDialogFragment(), CommunityUserAdapter.CommunityUserListener {

    companion object {
        var TAG = "AddMemberBottomSheetFragment"
    }

    private lateinit var binding: AddGroupMemberBottomSheetLayoutBinding
    private lateinit var db: FirebaseFirestore
    private var progressDialog: ProgressDialog? = null
    private var listener: BottomSheetListener? = null
    private var communityGroupId = ""
    private var adminID = ""

    private var userList = mutableListOf<GetUsers>()
    private var userListAdded = mutableListOf<MembersGroup>()
    private var selectedUsersList = mutableListOf<MembersGroup>()
    private var userAdapter: CommunityUserAdapter? = null
    private lateinit var viewModel: CommunityViewModel
    private var category = "Select category"

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
            Log.e(TAG, "Parent Fragment must implement BottomSheetListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AddGroupMemberBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root
        communityGroupId = arguments?.getString("communityGroupId").toString()
        adminID = arguments?.getString("adminId").toString()

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
       // setupAdapter()
        val adapter = ArrayAdapter(requireContext(), R.layout.spinner_list, Utils.categorySelect)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        binding.apply {
            btnAddMember.visibility = View.GONE
          //  spinnerCategory.visibility = View.GONE
            btnAddMember.setOnClickListener {
                try {
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

            spinnerCategory.adapter = adapter
            spinnerCategory.setSelection(Utils.categorySelect.indexOf(category))
            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (view != null) {
                        category = Utils.categorySelect[position]
                        val filtered = if (category == "Select category" || category == "All") {
                            getUserList
                        } else {
                            getUserList.filter {
                                it.category!!.contains(category, ignoreCase = true)
                            }
                        }
                        val selectAll = category != "Select category"
                        updateList(filtered, selectAll)
                    } else {
                        // handle the case where the view parameter is null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // write code to perform some action
                }
            }
        }

        lifecycleScope.launch {
            if(communityGroupId.isNotEmpty()) {
                val community = viewModel.getCommunitiesById(communityGroupId)
                community?.let {
                    userListAdded = it.members as ArrayList<MembersGroup>
                    Log.e("userListAdded", "${userListAdded.size}")
                } ?: run {
                    // Handle case where community is null (e.g., show a message)
                }
            }
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
    private fun updateList(users: List<GetUsers>, isSelectAll: Boolean) {
        try {
            selectedUsersList.clear()
            userAdapter!!.updateCategoryList(users, isSelectAll)
            users.forEach { user ->
                onCategorySelected(user)
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    private fun onCategorySelected(users: GetUsers) {
        var fcmToken = ""
        if(users.fcmToken != null){
            fcmToken = users.fcmToken!!
        }
        val members = MembersGroup(users.phone_number!!, users.name!!, users.phone_number, false, fcmToken)
        selectedUsersList.add(members)
        if(selectedUsersList.isNotEmpty()){
            binding.btnAddMember.visibility = View.VISIBLE
        } else {
            binding.btnAddMember.visibility = View.GONE
        }
    }
    private fun setupAdapter(){
        try{
            if (userList.isNotEmpty()) {
                val addedIds = userListAdded.map { it.id }.toSet()
                val filteredUsers = userList.filter { user ->
                    (userListAdded.isEmpty() || user.phone_number !in addedIds) &&
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
            userAdapter = CommunityUserAdapter(getUserList, this)
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
                    dismissProgDialog()
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
                    Log.e("userList", "${userList.size}")
                    Log.e("userListAdded", "${userListAdded.size}")
                    val duplicateUsers = userListAdded

                        .groupBy { it.phoneNumber }

                        .filter { it.value.size > 1 }

                    Log.e("DuplicateCount", duplicateUsers.size.toString())

                    duplicateUsers.forEach { (phone, users) ->

                        Log.e(

                            "DuplicateUser",

                            "Phone: $phone Count: ${users.size} Data: $users"

                        )

                    }
                    val duplicateTotal = userListAdded.size -

                            userListAdded.distinctBy { it.phoneNumber }.size

                    Log.e("DuplicateTotal", duplicateTotal.toString())
                    if(userList.isEmpty()){
                        binding.usersList.visibility = View.GONE
                        binding.noAddMembers.visibility = View.VISIBLE
                    } else {
                        binding.usersList.visibility = View.VISIBLE
                        binding.noAddMembers.visibility = View.GONE
                        setupAdapter()
                    }
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
        fun onAddMember(selectedUsersList: List<MembersGroup>)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onUserSelectListener(users: GetUsers, isSelected: Boolean) {
        var fcmToken = ""
        if(users.fcmToken != null){
            fcmToken = users.fcmToken!!
        }
        val members = MembersGroup(users.phone_number!!, users.name!!, users.phone_number, false, fcmToken)
        if (isSelected) {
            selectedUsersList.add(members)
        } else {
            if (selectedUsersList.contains(members)) {
                selectedUsersList.remove(members)
            }
        }
        Log.e(TAG,"selected users::: ${selectedUsersList.size}  FCM::: $fcmToken")
        if(selectedUsersList.isNotEmpty()){
            binding.btnAddMember.visibility = View.VISIBLE
        } else {
            binding.btnAddMember.visibility = View.GONE
        }
    }
}
