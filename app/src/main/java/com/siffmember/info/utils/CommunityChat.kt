package com.siffmember.info.utils

object CommunityChat {

    private var isChatOpen = false
    private var isReplyOpen = false

    private var fcmToken = ""

    fun setIsChatOpen(result: Boolean){
        this.isChatOpen = result
    }

    fun getIsChatOpen(): Boolean{
        return isChatOpen
    }

    fun setIsReplyOpen(result: Boolean){
        this.isReplyOpen = result
    }

    fun getIsReplyOpen(): Boolean{
        return isReplyOpen
    }

    fun setFCMToken(result: String){
        this.fcmToken = result
    }

    fun getFCMToken(): String{
        return fcmToken
    }

}