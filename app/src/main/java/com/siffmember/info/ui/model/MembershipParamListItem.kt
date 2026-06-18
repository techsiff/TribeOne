package com.siffmember.info.ui.model

sealed class MembershipParamListItem {
    data class Header(val category: String) : MembershipParamListItem()
    data class ParamsItem(val params: MembershipParamField) : MembershipParamListItem()
}