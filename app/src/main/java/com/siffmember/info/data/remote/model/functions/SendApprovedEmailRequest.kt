package com.siffmember.info.data.remote.model.functions

data class SendApprovedEmailRequest(val to: String, val subject: String, val message: String)