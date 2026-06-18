package com.siffmember.info.utils

object CallLogDetails {

    private var isCallInitiated: Boolean = false
    private var userName: String = ""
    private var userPhoneNumber: String = ""

    private var guestUserName: String = ""
    private var guestUserPhoneNumber: String = ""

    fun setCallInitiated(result: Boolean){
        this.isCallInitiated = result
    }

    fun getCallInitiated(): Boolean{
        return isCallInitiated
    }

    fun setUserName(result: String){
        this.userName = result
    }

    fun getUserName(): String{
        return userName
    }

    fun setUserPhoneNumber(result: String){
        this.userPhoneNumber = result
    }

    fun getUserPhoneNumber(): String{
        return userPhoneNumber
    }

    fun setGuestUserName(result: String){
        this.guestUserName = result
    }

    fun getGuestUserName(): String{
        return guestUserName
    }

    fun setGuestUserPhoneNumber(result: String){
        this.guestUserPhoneNumber = result
    }

    fun getGuestUserPhoneNumber(): String{
        return guestUserPhoneNumber
    }
}