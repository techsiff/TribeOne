package com.siffmember.info.ui.model

data class CategoryTwoList(
    val name: String? = null,
    val levelId: String = "",
    val createdBy: String = "",
    val allowedRoles: List<String> = listOf() // List of access roles allowed to access the content
)
