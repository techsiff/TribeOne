package com.siffmember.info.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.databinding.UserAccessBottomSheetLayoutBinding
import com.siffmember.info.ui.adapter.UserAccessAdapter
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.utils.Utils

class UserAccessBottomSheetFragment : BottomSheetDialogFragment(), UserAccessAdapter.UserAccessListener {

    companion object {
        var TAG = "UserAccessBottomSheetFragment"
    }

    private lateinit var binding: UserAccessBottomSheetLayoutBinding
    private var progressDialog: ProgressDialog? = null
    private var listener: BottomSheetListener? = null

    private var selectedUsersList = mutableListOf<String>()
    private var userAdapter: UserAccessAdapter? = null
    private lateinit var viewModel: CommunityViewModel

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
        binding = UserAccessBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        setupAdapter()
        binding.apply {
            btnAddMember.setOnClickListener {
                try {
                    listener!!.onUserAccessMember(selectedUsersList)
                    dismiss()
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

        }
    }

    private fun setupAdapter(){
        try{
            for (item in Utils.allowedRoles){
                selectedUsersList.add(item)
            }
            userAdapter = UserAccessAdapter(selectedUsersList, this)
            binding.userAccessList.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            binding.userAccessList.adapter = userAdapter
        }catch (e: Exception){
            e.printStackTrace()
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
        fun onUserAccessMember(selectedUsersList: List<String>)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }


    override fun onUserAccessSelectListener(users: String, isSelected: Boolean) {
        if (isSelected) {
            selectedUsersList.add(users)
        } else {
            if (selectedUsersList.contains(users)) {
                selectedUsersList.remove(users)
            }
        }
        Log.e(TAG,"selected users::: ${selectedUsersList.size}  users::: $users")
    }
}
