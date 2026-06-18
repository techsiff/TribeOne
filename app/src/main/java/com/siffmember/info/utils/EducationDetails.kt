package com.siffmember.info.utils

import com.siffmember.info.ui.model.CategoryList
import com.siffmember.info.ui.model.PlayLists
import com.siffmember.info.ui.model.SharedPlaylist
import com.siffmember.info.ui.model.VideoModel

object EducationDetails {

    private var videoDetails: VideoModel? = null
    private var videoFolderOne: CategoryList? = null
    private var videoFolderTwo: CategoryList? = null
    private var isPDFContentAdd: Boolean = false
    private var isVideoContentAdd: Boolean = false
    private var isContactUsAdd: Boolean = false
    private var playLists: PlayLists? = null
    private var sharedPlayLists: SharedPlaylist? = null

    fun setSharedPlayList(result: SharedPlaylist){
        this.sharedPlayLists = result
    }

    fun getSharedPlayList(): SharedPlaylist{
        return sharedPlayLists!!
    }

    fun setPlayList(result: PlayLists){
        this.playLists = result
    }

    fun getPlayList(): PlayLists{
        return playLists!!
    }

    fun setPDFContentAdd(result: Boolean){
        this.isPDFContentAdd = result
    }

    fun getPDFContentAdd(): Boolean{
        return isPDFContentAdd
    }

    fun setVideoContentAdd(result: Boolean){
        this.isVideoContentAdd = result
    }

    fun getVideoContentAdd(): Boolean{
        return isVideoContentAdd
    }

    fun setContactUsAdd(result: Boolean){
        this.isContactUsAdd = result
    }

    fun getContactUsAdd(): Boolean{
        return isContactUsAdd
    }

    fun setVideoDetails(result: VideoModel){
        this.videoDetails = result
    }

    fun getVideoDetails(): VideoModel{
        return videoDetails!!
    }

    fun setVideoFolderOne(result: CategoryList){
        this.videoFolderOne = result
    }

    fun getVideoFolderOne(): CategoryList{
        return videoFolderOne!!
    }

    fun setVideoFolderTwo(result: CategoryList){
        this.videoFolderTwo = result
    }

    fun getVideoFolderTwo(): CategoryList{
        return videoFolderTwo!!
    }
}