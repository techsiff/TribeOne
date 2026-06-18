package com.siffmember.info.ui.model

data class CategoryList(
    val name: String? = null,
    val levelId: String = "",
    val createdBy: String = "",
    val allowedRoles: List<String> = listOf()
)
