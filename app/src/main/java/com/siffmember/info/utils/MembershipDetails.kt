package com.siffmember.info.utils

import com.siffmember.info.ui.model.Membership

object MembershipDetails {

    private var user: Membership? = null

    fun setMembershipDetails(result: Membership){
        this.user = result
    }

    fun getMembershipDetails(): Membership{
        return user!!
    }
}