package com.siffmember.info.utils

import com.siffmember.info.ui.model.MeetingAnalyticsModel
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.ui.model.MembersZoomMeeting

object MeetingUserDetails {

    private val userList = ArrayList<MembersZoomMeeting>()
    private var meetingConfigDetails: MeetingHomeDetailsModel? = null
    private var meetingDetails: MeetingDetailsModel? = null
    private var meetingAnalyticsDetails: MeetingAnalyticsModel? = null


    fun setUsers(users: List<MembersZoomMeeting>) {
        userList.clear()
        userList.addAll(users)
    }

    fun getUsers(): List<MembersZoomMeeting> {
        return userList.toList() // immutable copy
    }

    fun addUser(user: MembersZoomMeeting) {
        userList.add(user)
    }

    fun removeUserById(userId: String) {
        userList.removeAll { it.id == userId }
    }

    fun clear() {
        userList.clear()
    }

    fun setMeetingConfigDetails(result: MeetingHomeDetailsModel){
        this.meetingConfigDetails = result
    }

    fun getMeetingConfigDetails(): MeetingHomeDetailsModel{
        return meetingConfigDetails!!
    }

    fun setMeetingDetails(result: MeetingDetailsModel){
        this.meetingDetails = result
    }

    fun getMeetingDetails(): MeetingDetailsModel{
        return meetingDetails!!
    }

    fun setMeetingAnalyticsDetails(result: MeetingAnalyticsModel){
        this.meetingAnalyticsDetails = result
    }

    fun getMeetingAnalyticsDetails(): MeetingAnalyticsModel{
        return meetingAnalyticsDetails!!
    }
}
