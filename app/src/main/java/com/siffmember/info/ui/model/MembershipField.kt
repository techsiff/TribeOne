package com.siffmember.info.ui.model

import android.text.InputType

data class MembershipField(
    val key: String,
    val label: String,
    val isRequired: Boolean = false,
    val category: String,
    val inputType: Int = InputType.TYPE_CLASS_TEXT
)

