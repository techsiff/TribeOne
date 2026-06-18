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
import com.siffmember.info.databinding.MembershipParamBottomSheetLayoutBinding
import com.siffmember.info.utils.Utils

class AddMembershipParamsBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "AddMembershipParamsBottomSheetFragment"
    }

    private lateinit var binding: MembershipParamBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null
    private var paramName = ""
    private var isMandatory = false
    private var category = "Personal Info"

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
        binding = MembershipParamBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(requireActivity(), R.layout.spinner_list, Utils.ParamCategory)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        binding.apply {
            btnAddAnnounce.setOnClickListener {
                try {
                    if(validate()){
                        listener!!.onAddParamName(paramName.trimEnd(), isMandatory, category)
                        dismiss()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

            radioGroupParam.setOnCheckedChangeListener { _, checkedId ->
                val value = when (checkedId) {
                    R.id.paramYes -> "Yes"
                    R.id.paramNo -> "No"
                    else -> ""
                }
                isMandatory = value == "Yes"
                Log.d("Selected", value)
            }

            spinnerCategory.adapter = adapter

            spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (view != null) {
                        category = Utils.ParamCategory[position]
                    } else {
                        // handle the case where the view parameter is null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // write code to perform some action
                }
            }
        }
    }

    private fun validate(): Boolean {
        if(binding.paramNameEdit.text.toString().isNotEmpty()){
            paramName = binding.paramNameEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(), "Please enter param name", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }


    interface BottomSheetListener {
        fun onAddParamName(paramName: String, isMandatory: Boolean, category: String)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
