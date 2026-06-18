package com.siffmember.info.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.R
import com.siffmember.info.databinding.CreateTagBottomSheetLayoutBinding
import com.siffmember.info.databinding.MembershipParamBottomSheetLayoutBinding
import com.siffmember.info.utils.Utils

class AddTagsBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "AddTagsBottomSheetFragment"
    }

    private lateinit var binding: CreateTagBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null
    private var tagName = ""
    private var addType = ""

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
        binding = CreateTagBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root
        addType = arguments?.getString("addType").toString()
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(requireActivity(), R.layout.spinner_list, Utils.ParamCategory)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        binding.apply {
            if(addType == "1"){
                titleTag.text = "Enter the tag name"
                tagNameEdit.setHint("Tag Name")
            } else if(addType == "2"){
                titleTag.text = "Enter the questionnaire name"
                tagNameEdit.setHint("Questionnaire name")
            } else if(addType == "3"){
                titleTag.text = "Enter the question"
                tagNameEdit.setHint("Enter the question")
            }

            btnAddTag.setOnClickListener {
                try {
                    if(validate()){
                        listener!!.onAddTagName(tagName)
                        dismiss()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun validate(): Boolean {
        if(binding.tagNameEdit.text.toString().isNotEmpty()){
            tagName = binding.tagNameEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(), "Please enter tag name", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }


    interface BottomSheetListener {
        fun onAddTagName(tagName: String)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
