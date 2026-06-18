package com.siffmember.info.ui.model

data class EducationContentAccess(
    val name: String = "",
    val createdBy: String = "",
    val allowedRoles: List<String> = listOf() // List of access roles allowed to access the content
)
