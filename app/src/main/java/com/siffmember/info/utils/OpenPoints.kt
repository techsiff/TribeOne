package com.siffmember.info.utils

import com.siffmember.info.ui.model.OpenPointDetails
import com.siffmember.info.ui.model.OpenPointList


object OpenPoints {

    private var apiKey: String = ""
    private var driveFolderId: String = ""
    private var nextSL: String = ""
    private var opList: OpenPointList? = null
    private var opDetails: OpenPointDetails? = null
    private var isEditOPDetails = false
    private var isGuestUser = false


    fun setApiKey(result: String){
        this.apiKey = result
    }

    fun getApiKey(): String{
        return apiKey
    }

    fun setDriveFolderId(result: String){
        this.driveFolderId = result
    }

    fun getDriveFolderId(): String{
        return driveFolderId
    }

    fun setNextSL(result: String){
        this.nextSL = result
    }

    fun getNextSL(): String{
        return nextSL
    }

    fun setIsEditOPDetails(result: Boolean){
        this.isEditOPDetails = result
    }

    fun getIsEditOPDetails(): Boolean{
        return isEditOPDetails
    }

    fun setOpenPointsDetails(result: OpenPointDetails){
        this.opDetails = result
    }

    fun getOpenPointsDetails(): OpenPointDetails {
        return opDetails!!
    }

    fun setOpenPointsList(result: OpenPointList){
        this.opList = result
    }

    fun getOpenPointsList(): OpenPointList {
        return opList!!
    }

    fun setIsGuestUser(result: Boolean){
        this.isGuestUser = result
    }

    fun getIsGuestUser(): Boolean{
        return isGuestUser
    }
}