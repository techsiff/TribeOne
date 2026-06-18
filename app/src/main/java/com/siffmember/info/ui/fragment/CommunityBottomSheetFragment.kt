package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.R
import com.siffmember.info.databinding.CommunityBottomSheetLayoutBinding

class CommunityBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "CommunityBottomSheetFragment"
    }

    private lateinit var binding: CommunityBottomSheetLayoutBinding
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
        binding = CommunityBottomSheetLayoutBinding.inflate(inflater, container, false)
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

            announceDescEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

                @SuppressLint("SetTextI18n")
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    val length = s?.length ?: 0
                    charCounter.text = "$length/550"
                    // Optional: turn red when limit reached
                    if (length >= 550) {
                        charCounter.setTextColor(ContextCompat.getColor(requireActivity(), R.color.red))
                    } else {
                        charCounter.setTextColor(ContextCompat.getColor(requireActivity(), R.color.grey))
                    }
                }

                override fun afterTextChanged(s: Editable?) {}
            })
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
