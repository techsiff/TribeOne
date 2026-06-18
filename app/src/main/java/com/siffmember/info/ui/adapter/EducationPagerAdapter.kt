package com.siffmember.info.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.siffmember.info.ui.fragment.PDFFragment
import com.siffmember.info.ui.fragment.PDFMainFragment
import com.siffmember.info.ui.fragment.VideoFragment
import com.siffmember.info.ui.fragment.VideoMainFragment

class EducationPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> VideoMainFragment()
            1 -> PDFMainFragment()
            else -> VideoMainFragment()
        }
    }

}