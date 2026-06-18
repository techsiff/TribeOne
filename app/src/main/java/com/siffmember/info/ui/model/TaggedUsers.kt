package com.siffmember.info.ui.model

import androidx.annotation.Keep

@Keep
data class TaggedUsers(
    val title: String = "",
    val users: List<MembersGroup>
)
