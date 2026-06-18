package com.siffmember.info.data.remote.model.openpoints

data class CreateSheetRequest(val folderId: String, val title: String,  val newData: List<List<String>>)
