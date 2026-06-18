package com.siffmember.info.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_details")
data class UserDetailsEntity(
    @PrimaryKey val phone_number: String,
    val name: String,
    val email_id: String,
    val country: String,
    val category: String
)
