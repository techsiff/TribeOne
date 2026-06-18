package com.siffmember.info.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_tags")
data class CategoryTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val communityId: String,  // Foreign key reference
    val tagName: String
)
