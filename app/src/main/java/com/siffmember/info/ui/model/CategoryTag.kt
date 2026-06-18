package com.siffmember.info.ui.model

data class CategoryTag(
    val name: String = "",
    val communities: List<String> = listOf() // List of community IDs in this category
)