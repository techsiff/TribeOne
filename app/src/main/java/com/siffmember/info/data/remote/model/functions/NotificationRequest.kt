package com.siffmember.info.data.remote.model.functions

data class NotificationRequest(
    val tokens: List<String>? = null,
    val token: String? = null,       // Token for a single device
    val topic: String? = null,       // Topic for the broadcast
    val title: String,
    val message: String,
    val customData: Map<String, String>? = null // Optional custom data
)
