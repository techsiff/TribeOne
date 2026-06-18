package com.siffmember.info.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.databinding.UpdateGroupNameBottomSheetLayoutBinding

class UpdateGroupNameBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "UpdateGroupNameBottomSheetFragment"
    }

    private lateinit var binding: UpdateGroupNameBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null
    private var newGroupName = ""
    private var oldGroupName = ""

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
        binding = UpdateGroupNameBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root
        oldGroupName = arguments?.getString("oldGroupName").toString()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            groupNameUpdateEdit.setText(oldGroupName)
            btnUpdateGn.setOnClickListener {
                try {
                    if(validate()){
                        listener!!.onUpdateGroupName(newGroupName)
                        dismiss()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun validate(): Boolean {
        if(binding.groupNameUpdateEdit.text.toString().isNotEmpty()){
            newGroupName = binding.groupNameUpdateEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(), "Please enter new group name", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }


    interface BottomSheetListener {
        fun onUpdateGroupName(groupName: String)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
