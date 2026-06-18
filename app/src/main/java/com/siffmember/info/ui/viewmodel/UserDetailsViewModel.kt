package com.siffmember.info.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.siffmember.info.data.local.entity.DeleteMessages
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.data.local.entity.UserDetailsEntity
import com.siffmember.info.data.local.repository.PostMessageRepository
import com.siffmember.info.data.local.repository.UserDetailsRepository
import com.siffmember.info.ui.model.PostWithReplyCount
import kotlinx.coroutines.launch

class UserDetailsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserDetailsRepository = UserDetailsRepository(application)

    fun insertUserDetails(user: UserDetailsEntity){
        repository.insertUserDetails(user)
    }

    fun getUserDetails(userId: String): LiveData<UserDetailsEntity?> {
        return repository.getUserDetails(userId)
    }

    fun deleteUserDetails() {
        viewModelScope.launch {
            repository.deleteUserDetails()
        }
    }
}