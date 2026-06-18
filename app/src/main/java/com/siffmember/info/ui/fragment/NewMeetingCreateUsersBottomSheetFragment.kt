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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.databinding.AddMemberBottomSheetLayoutBinding
import com.siffmember.info.ui.adapter.MeetingCreateUsersAdapter
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.utils.MeetingCreateUserDetails

class NewMeetingCreateUsersBottomSheetFragment : BottomSheetDialogFragment(), MeetingCreateUsersAdapter.MeetingCreateUsersListener {

    companion object {
        var TAG = "NewCreateUsersBottomSheetFragment"
    }

    private lateinit var binding: AddMemberBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null
    private var usersList = mutableListOf<MembersGroup>()
    private var meetingCreateUsers: MeetingCreateUsersAdapter? = null
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AddMemberBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root
        adminID = arguments?.getString("adminId").toString()

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupAdapter()
        binding.apply {
            btnAddMember.visibility = View.GONE
            noAddMembers.visibility = View.GONE
            spinnerCategory.visibility = View.GONE
            addMemberTitle.text = "Update users list"
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
            usersList  // Reset to full list when query is empty
        } else {
            usersList.filter {
                it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query, ignoreCase = true)
            }
        }
        meetingCreateUsers!!.updateList(filteredVideos)
    }

    private fun setupAdapter(){
        try{
            usersList = MeetingCreateUserDetails.getUsers() as MutableList<MembersGroup>
            meetingCreateUsers = MeetingCreateUsersAdapter(usersList, this)
            binding.usersList.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            binding.usersList.adapter = meetingCreateUsers
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    interface BottomSheetListener {
        fun onUpdateMember()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onUserUpdateListener(users: MembersGroup) {
        MeetingCreateUserDetails.removeUserById(users.phoneNumber)
        setupAdapter()
        listener!!.onUpdateMember()
    }

}
