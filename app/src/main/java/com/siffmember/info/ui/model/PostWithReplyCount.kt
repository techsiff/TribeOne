package com.siffmember.info.ui.model

import androidx.room.Embedded
import com.siffmember.info.data.local.entity.PostMessage

data class PostWithReplyCount(
    @Embedded val post: PostMessage,
    val replyCount: Int
)