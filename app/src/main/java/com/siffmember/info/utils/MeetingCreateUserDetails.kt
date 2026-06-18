package com.siffmember.info.utils

import com.siffmember.info.ui.model.MembersGroup

object MeetingCreateUserDetails {

    private val userList = ArrayList<MembersGroup>()

//    fun setUsers(users: List<MembersGroup>) {
//        userList.clear()
//        userList.addAll(users)
//    }

    fun getUsers(): List<MembersGroup> {
        return userList.toList() // immutable copy
    }

    fun addUser(user: MembersGroup) {
        userList.add(user)
    }

    fun removeUserById(userId: String) {
        userList.removeAll { it.id == userId }
    }

    fun clear() {
        userList.clear()
    }
}
