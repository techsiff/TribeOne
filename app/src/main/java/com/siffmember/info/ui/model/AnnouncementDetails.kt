package com.siffmember.info.ui.model

import androidx.annotation.Keep

@Keep
data class AnnouncementDetails(
    val dateTime: String? = null,
    val title: String? = null,
    val description: String? = null
)
