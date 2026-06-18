package com.siffmember.info.ui.model

import androidx.annotation.Keep

@Keep
data class MembersGroup(
    val id: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val isAdmin: Boolean = false,
    val fcmToken: String = ""
)
