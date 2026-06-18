package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.AddMemberBottomSheetLayoutBinding
import com.siffmember.info.ui.adapter.DeleteStoryAdapter
import com.siffmember.info.ui.model.UploadStoryFile
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.utils.UserStory

class DeleteStoryBottomSheet : BottomSheetDialogFragment(), DeleteStoryAdapter.DeleteStoryListener {

    companion object {
        var TAG = "DeleteStoryBottomSheet"
    }

    private lateinit var binding: AddMemberBottomSheetLayoutBinding
    private lateinit var db: FirebaseFirestore
    private var progressDialog: ProgressDialog? = null
    private var listener: BottomSheetListener? = null
    private var adminID = ""

    private var userList = mutableListOf<UploadStoryFile>()
    private var userAdapter: DeleteStoryAdapter? = null

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

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore
        progressDialog = ProgressDialog(requireActivity())
        userList = UserStory.getUserStoryModel().storyList

        binding.apply {
            addMemberTitle.text = "${UserStory.getUserStoryModel().userName} story list"
            btnAddMember.visibility = View.GONE
            spinnerCategory.visibility = View.GONE
            userSearchEdit.visibility = View.GONE
        }
        setupAdapter()
    }

    private fun setupAdapter(){
        try{
            Log.e(TAG,"${userList.size}")
            if(userList.isEmpty()){
                binding.usersList.visibility = View.GONE
                binding.noAddMembers.visibility = View.VISIBLE
            } else {
                binding.usersList.visibility = View.VISIBLE
                binding.noAddMembers.visibility = View.GONE
            }
            userAdapter = DeleteStoryAdapter(userList, this)
            binding.usersList.layoutManager = LinearLayoutManager(requireActivity(), LinearLayoutManager.VERTICAL, false)
            binding.usersList.adapter = userAdapter
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
        fun onSelectedQuestion(user: UploadStoryFile)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onDeleteStoryListener(users: UploadStoryFile, isSelected: Boolean) {
        try {
            listener!!.onSelectedQuestion(users)
            dismiss()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
}
