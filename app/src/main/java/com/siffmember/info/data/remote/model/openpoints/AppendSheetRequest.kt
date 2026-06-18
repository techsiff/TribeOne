package com.siffmember.info.data.remote.model.openpoints

data class AppendSheetRequest(val spreadsheetId: String, val sheetName: String, val newData: List<List<String>>)
