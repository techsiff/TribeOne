package com.siffmember.info.ui.model

data class UploadStoryFile(
    val id: String = "",
    val fileName: String = "",
    val storyFile: String = "",
    val mimeType: String = "",
    val timestamp: String = ""
){
    override fun toString(): String {
        return fileName // what Spinner displays
    }
}
