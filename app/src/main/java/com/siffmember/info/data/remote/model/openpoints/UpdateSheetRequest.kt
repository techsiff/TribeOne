package com.siffmember.info.data.remote.model.openpoints

data class UpdateSheetRequest(val spreadsheetId: String, val sheetName: String, val newData: List<List<String>>)