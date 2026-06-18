package com.siffmember.info.data.remote.model.openpoints

data class ReadSheetData(
    val rowIndex: Int,
    val sl: String,
    val description: String,
    val who: String,
    val remarks: String,
    val status: String
)