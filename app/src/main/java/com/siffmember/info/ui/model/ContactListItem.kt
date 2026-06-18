package com.siffmember.info.ui.model

sealed class ContactListItem {
    data class Header(val state: String) : ContactListItem()
    data class ContactItem(val contact: ContactusDetails) : ContactListItem()
}