package com.siffmember.info.data.local.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.siffmember.info.ui.model.MembersGroup

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromMembersGroupList(members: List<MembersGroup>): String {
        return gson.toJson(members) // Convert List<MembersGroup> to JSON String
    }

    @TypeConverter
    fun toMembersGroupList(data: String): List<MembersGroup> {
        val listType = object : TypeToken<List<MembersGroup>>() {}.type
        return gson.fromJson(data, listType) ?: emptyList() // Convert JSON String back to List<MembersGroup>
    }
}