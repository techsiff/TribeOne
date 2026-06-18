package com.siffmember.info.utils

import com.siffmember.info.ui.model.UserStoryModel

object UserStory {

    private var storyFile: String = ""
    private var mime: String = ""
    private var questions: String = ""
    private var userStoryModel: UserStoryModel? = null

    fun setUserStoryModel(result: UserStoryModel){
        this.userStoryModel = result
    }

    fun getUserStoryModel(): UserStoryModel{
        return userStoryModel!!
    }

    fun setStoryFile(result: String){
        this.storyFile = result
    }

    fun getStoryFile(): String{
        return storyFile
    }

    fun setMime(result: String){
        this.mime = result
    }

    fun getMime(): String{
        return mime
    }

    fun setQuestions(result: String){
        this.questions = result
    }

    fun getQuestions(): String{
        return questions
    }

}