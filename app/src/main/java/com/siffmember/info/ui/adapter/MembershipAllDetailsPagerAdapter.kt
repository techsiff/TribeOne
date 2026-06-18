package com.siffmember.info.ui.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.siffmember.info.ui.fragment.MembershipAllDetailsFragment

class MembershipAllDetailsPagerAdapter(
    activity: FragmentActivity,
    private val categoryList: List<Pair<String, Map<String, String>>>,
    private val memberId: String
) : FragmentStateAdapter(activity) {

    override fun getItemCount() = categoryList.size

    override fun createFragment(position: Int): Fragment {
        val (categoryName, fields) = categoryList[position]
        return MembershipAllDetailsFragment.newInstance(
            categoryName,
            memberId,
            HashMap(fields) // Convert Map → HashMap
        )
    }
    /*override fun createFragment(position: Int): Fragment {
        return if (position == categoryList.size) {
            // Last Fragment → Notes
            MembershipDetailsThreeFragment.newInstance(memberId)
        } else {
            val (categoryName, fields) = categoryList[position]
            MembershipAllDetailsFragment.newInstance(
                categoryName,
                HashMap(fields)
            )
        }
    }*/
}
