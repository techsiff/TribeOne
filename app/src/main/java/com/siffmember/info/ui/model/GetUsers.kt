package com.siffmember.info.ui.model

data class GetUsers(
    val name: String? = null,
    val email_id: String? = null,
    val country: String? = null,
    val phone_number: String? = null,
    var category: String? = null,
    var fcmToken: String? = null  // Optional field
)
