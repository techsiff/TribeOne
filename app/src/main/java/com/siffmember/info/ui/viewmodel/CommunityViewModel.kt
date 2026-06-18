package com.siffmember.info.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.siffmember.info.data.local.database.AppDatabase
import com.siffmember.info.data.local.entity.CategoryTagEntity
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.data.local.repository.CommunityRepository
import com.siffmember.info.ui.model.CommunityModel
import com.siffmember.info.ui.model.LastChatsModel
import com.siffmember.info.ui.model.MembersGroup
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch

class CommunityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CommunityRepository

    private val _groupsWithLastPost = MutableLiveData<List<CommunityModel>>()
    val groupsWithLastPost: LiveData<List<CommunityModel>> = _groupsWithLastPost

    init {
        val communityDao = AppDatabase.getDatabase(application).communityDao()
        repository = CommunityRepository(communityDao)
    }

    val allCommunities: LiveData<List<CommunityEntity>> = repository.allCommunities.asLiveData()

    fun insertCommunity(community: CommunityEntity) {
        viewModelScope.launch {
            repository.insertCommunity(community)
        }
    }

    fun insertAllCommunity(community: List<CommunityEntity>) {
        viewModelScope.launch {
            repository.insertAllCommunity(community)
        }
    }

    suspend fun getCommunitiesById(groupId: String): CommunityEntity? {
        return repository.getCommunitiesById(groupId)
    }

    fun getAllCommunities(): List<CommunityEntity>{
        return repository.getAllCommunities()
    }
    fun getAllCommunitiesIdList(): List<String>{
        return repository.getAllCommunitiesIdList()
    }

    fun getCommunitiesMemberById(groupId: String): LiveData<CommunityEntity?> {
        return repository.getCommunitiesMemberById(groupId)
    }

    fun updateFilePath(groupID: String, filePath: String) {
        viewModelScope.launch {
            repository.updateFilePath(groupID, filePath)
        }
    }

    fun updateGroupStatus(groupId: String, groupStatus: Boolean) {
        viewModelScope.launch {
            repository.updateGroupStatus(groupId, groupStatus)
        }
    }

    fun updateGroupMembers(groupId: String, newMembers: List<MembersGroup>) {
        viewModelScope.launch {
            repository.updateGroupMembers(groupId, newMembers)
        }
    }

    fun updateGroupDetails(groupId: String, groupName: String, description: String, groupIcon: String) {
        viewModelScope.launch {
            repository.updateGroupDetails(groupId, groupName, description, groupIcon)
        }
    }

    fun deleteCommunitiesById(groupId: String) {
        viewModelScope.launch {
            repository.deleteCommunityByID(groupId)
        }
    }

    fun deleteAllCommunities() {
        viewModelScope.launch {
            repository.deleteAllCommunity()
        }
    }


    fun fetchGroupsWithLastPost(chatViewModel: PostsMessageViewModel) {
        allCommunities.observeForever { communities ->
            if (communities.isEmpty()) {
                _groupsWithLastPost.postValue(emptyList()) // Clear the list when no data
                return@observeForever
            }
            val updatedGroups = mutableListOf<CommunityModel>()
            val latch = CountDownLatch(communities.size)

            for (group in communities) {
                chatViewModel.getGroupLastPostMessages(group.groupID).observeForever { messages ->
                    val lastChat = messages?.let {
                        LastChatsModel(
                            groupName = messages.groupName,
                            groupId = messages.groupId,
                            content = messages.content,
                            timestamp = messages.timestamp,
                            userName = messages.userName,
                            userId = messages.userId,
                            isSent = false
                        )
                    } ?: LastChatsModel("", "", "", "", "", "", false)

                    updatedGroups.add(
                        CommunityModel(
                            group.groupID, group.groupName, group.description,
                            group.createdBy, group.createdAt, group.groupIcon, lastChat
                        )
                    )

                    latch.countDown()
                    if (latch.count == 0L) {
                        // Sort by timestamp in descending order (latest chat first)
                        updatedGroups.sortByDescending {
                            it.chats.timestamp.toLongOrNull() ?: 0L
                        }
                        _groupsWithLastPost.postValue(updatedGroups)
                    }
                }
            }
        }
    }

    // Category Tag Queries
    fun insertCategoryTag(tag: CategoryTagEntity) {
        viewModelScope.launch {
            repository.insertCategoryTag(tag)
        }
    }

    val allTags: LiveData<List<String>> = repository.allTags.asLiveData()

    fun getCommunitiesByTag(tag: String): LiveData<List<CommunityEntity>> {
        return repository.getCommunitiesByTag(tag).asLiveData()
    }

    fun fetchGroupsWithLastPostByTag(chatViewModel: PostsMessageViewModel, tag: String) {
        Log.e("CommunityViewModel", "fetchGroupsWithLastPostByTag called")
        getCommunitiesByTag(tag).observeForever { communities ->
            if (communities.isEmpty()) {
                _groupsWithLastPost.postValue(emptyList()) // Clear the list when no data
                return@observeForever
            }
            val updatedGroups = mutableListOf<CommunityModel>()
            val latch = CountDownLatch(communities.size)

            for (group in communities) {
                chatViewModel.getGroupLastPostMessages(group.groupID).observeForever { messages ->
                    val lastChat = messages?.let {
                        LastChatsModel(
                            groupName = messages.groupName,
                            groupId = messages.groupId,
                            content = messages.content,
                            timestamp = messages.timestamp,
                            userName = messages.userName,
                            userId = messages.userId,
                            isSent = false
                        )
                    } ?: LastChatsModel("", "", "", "", "", "", false)

                    updatedGroups.add(
                        CommunityModel(
                            group.groupID, group.groupName, group.description,
                            group.createdBy, group.createdAt, group.groupIcon, lastChat
                        )
                    )

                    latch.countDown()
                    if (latch.count == 0L) {
                        // Sort by timestamp in descending order (latest chat first)
                        updatedGroups.sortByDescending {
                            it.chats.timestamp.toLongOrNull() ?: 0L
                        }
                        _groupsWithLastPost.postValue(updatedGroups)
                    }
                }
            }
        }
    }

    fun getTotalCategoryCount(callback: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.getTotalCategoryCount()
            callback(count)
        }
    }

    fun doesCategoryExist(tagName: String, groupId: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            val exists = repository.doesCategoryExist(tagName, groupId)
            callback(exists)
        }
    }

    fun deleteCategoryTagByName(tagName: String) {
        viewModelScope.launch {
            repository.deleteCategoryTagByName(tagName)
        }
    }

    fun deleteAllCategoryTag() {
        viewModelScope.launch {
            repository.deleteAllCategoryTag()
        }
    }
}