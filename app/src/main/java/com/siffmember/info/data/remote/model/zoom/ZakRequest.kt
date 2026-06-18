package com.siffmember.info.data.remote.model.zoom

data class ZakRequest(val userIdOrEmail: String,
                      val clientId: String,
                      val clientSecret: String,
                      val accountId: String
)