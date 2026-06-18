package com.siffmember.info.ui.model

import androidx.annotation.Keep

@Keep
data class SharedPlaylist(
    val playlistId: String = "",
    val name: String = "",
    val sharedUserId: String = "",
    val sharedUserName: String = "",
)
