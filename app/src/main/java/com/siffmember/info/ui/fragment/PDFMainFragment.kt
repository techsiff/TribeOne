package com.siffmember.info.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siffmember.info.databinding.FragmentPdfMainBinding
import com.siffmember.info.databinding.FragmentVideosMainBinding

class PDFMainFragment : BaseFragment() {

    companion object {
       // private const val TAG = "VideoMainFragment"
    }

    private lateinit var binding: FragmentPdfMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentPdfMainBinding.inflate(layoutInflater)
        return binding.root
    }

}