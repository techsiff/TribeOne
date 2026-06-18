package com.siffmember.info.ui.fragment

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.databinding.ForgotPasswordBottomSheetLayoutBinding


class ForgotPasswordBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "ForgotPasswordBottomSheetFragment"
    }

    private lateinit var binding: ForgotPasswordBottomSheetLayoutBinding
    private var listener: ForgotPasswordBottomSheetListener? = null
    private var email = ""

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
        if (context is ForgotPasswordBottomSheetListener) {
            listener = context
        } else {
            Log.e(TAG, "Parent activity must implement BottomSheetListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = ForgotPasswordBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            btnLoginPassword.setOnClickListener {
                try {
                    if(validate()){
                        listener!!.onSendRestPassword(email)
                        dismiss()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            loginBackEp.setOnClickListener {
                dismiss()
            }
        }
    }

    private fun validate(): Boolean {
        if(binding.loginEmailEdit.text.toString().isNotEmpty()){
            email = binding.loginEmailEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(), "Please enter email id", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    interface ForgotPasswordBottomSheetListener {
        fun onSendRestPassword(emailId: String)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
