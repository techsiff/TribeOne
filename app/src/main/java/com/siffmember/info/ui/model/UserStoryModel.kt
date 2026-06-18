package com.siffmember.info.ui.model

data class UserStoryModel(
    var userName: String = "",
    var userId: String = "",
    var storyList: ArrayList<UploadStoryFile> = ArrayList()
)