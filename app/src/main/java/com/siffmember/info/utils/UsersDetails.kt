package com.siffmember.info.utils

import com.siffmember.info.ui.model.AnnouncementDetails
import com.siffmember.info.ui.model.GetUsers
import com.siffmember.info.ui.model.UsersRegistration

object UsersDetails {

    private var user: GetUsers? = null
    private var registeredUser: UsersRegistration? = null
    private var announcementDetail: AnnouncementDetails? = null
    private var isEditUserDetails = false

    fun setAnnouncementDetail(result: AnnouncementDetails){
        this.announcementDetail = result
    }

    fun getAnnouncementDetail(): AnnouncementDetails{
        return announcementDetail!!
    }

    fun setIsEditUserDetails(result: Boolean){
        this.isEditUserDetails = result
    }

    fun getIsEditUserDetails(): Boolean{
        return isEditUserDetails
    }


    fun setUsersDetails(result: GetUsers){
        this.user = result
    }

    fun getUsersDetails(): GetUsers{
        return user!!
    }

    fun setUsersRegisteredDetails(result: UsersRegistration){
        this.registeredUser = result
    }

    fun getUsersRegisteredDetails(): UsersRegistration{
        return registeredUser!!
    }
}