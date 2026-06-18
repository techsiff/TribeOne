package com.siffmember.info.data.local.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.siffmember.info.data.local.database.AppDatabase
import com.siffmember.info.data.local.entity.DeleteMessages
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.data.local.entity.UserDetailsEntity
import com.siffmember.info.ui.model.PostWithReplyCount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UserDetailsRepository(context: Context) {

    //Post messages
    private val dbPost: AppDatabase = AppDatabase.getDatabase(context)
    private val userDao = dbPost.userDao()

    fun insertUserDetails(user: UserDetailsEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            userDao.insertUserDetails(user)
        }
    }

    fun getUserDetails(userId: String): LiveData<UserDetailsEntity?> = userDao.getUserDetails(userId)

    suspend fun deleteUserDetails() {
        userDao.deleteUserDetails()
    }
}