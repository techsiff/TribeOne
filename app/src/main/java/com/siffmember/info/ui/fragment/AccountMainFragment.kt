package com.siffmember.info.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.siffmember.info.databinding.FragmentAccountMainBinding


class AccountMainFragment : BaseFragment() {

    companion object {
        private const val TAG = "AccountMainFragment"
    }

    private lateinit var binding: FragmentAccountMainBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        binding = FragmentAccountMainBinding.inflate(layoutInflater)
        return binding.root
    }

}