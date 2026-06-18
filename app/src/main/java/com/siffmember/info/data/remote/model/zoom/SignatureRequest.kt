package com.siffmember.info.data.remote.model.zoom

data class SignatureRequest(val meetingNumber: String,
                            val role: String,
                            val clientId: String,
                            val clientSecret: String
)