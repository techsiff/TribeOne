package com.siffmember.info.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.databinding.AnnounceBottomSheetLayoutBinding

class AnnounceBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "AnnounceBottomSheetFragment"
    }

    private lateinit var binding: AnnounceBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null
    private var title = ""
    private var description = ""

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
        binding = AnnounceBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            btnAddAnnounce.setOnClickListener {
                try {
                    if(validate()){
                        listener!!.onAddAnnouncement(title, description)
                        dismiss()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun validate(): Boolean {
        if(binding.announceTitleEdit.text.toString().isNotEmpty()){
            title = binding.announceTitleEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(), "Please enter announcement title", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.announceDescEdit.text.toString().isNotEmpty()){
            description = binding.announceDescEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(), "Please enter announcement description", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    interface BottomSheetListener {
        fun onAddAnnouncement(title: String, description: String)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
