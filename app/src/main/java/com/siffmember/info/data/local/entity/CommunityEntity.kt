package com.siffmember.info.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.siffmember.info.data.local.database.Converters
import com.siffmember.info.ui.model.MembersGroup

@Entity(tableName = "communities")
data class CommunityEntity(
    @PrimaryKey val groupID: String,  // Now acts as a unique identifier
    val groupName: String,
    val description: String,
    val createdBy: String,
    val createdAt: String = "",
    val groupIcon: String = "",
    val groupStatus: Boolean = false,
    @TypeConverters(Converters::class)
    val members: List<MembersGroup> = listOf()
)