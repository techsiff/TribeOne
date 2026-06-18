package com.siffmember.info.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.databinding.PlaylistShareBottomSheetLayoutBinding

class SharedPlaylistBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "SharedPlaylistBottomSheetFragment"
    }

    private lateinit var binding: PlaylistShareBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null
    private var phoneNumber = ""

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the parent activity implements the interface
        if (parentFragment is BottomSheetListener) {
            listener = parentFragment as BottomSheetListener
        } else if (context is BottomSheetListener) {
            listener = context as BottomSheetListener
        } else {
            Log.e(TAG, "Parent activity must implement BottomSheetListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PlaylistShareBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            btnSearchUser.setOnClickListener {
                try {
                    if(validate()){
                        listener!!.onSearchUser(phoneNumber)
                        dismiss()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private fun validate(): Boolean {
        if(binding.shareContactEdit.text.toString().isNotEmpty()){
            phoneNumber = binding.shareContactEdit.text.toString()
        } else {
            Toast.makeText(requireActivity(), "Please enter user phone number", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    interface BottomSheetListener {
        fun onSearchUser(phoneNumber: String)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}