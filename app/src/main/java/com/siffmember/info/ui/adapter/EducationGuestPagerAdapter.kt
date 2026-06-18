package com.siffmember.info.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.siffmember.info.ui.fragment.PDFFragment
import com.siffmember.info.ui.fragment.VideoFragment

class EducationGuestPagerAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> VideoFragment()
            1 -> PDFFragment()
            else -> VideoFragment()
        }
    }

}