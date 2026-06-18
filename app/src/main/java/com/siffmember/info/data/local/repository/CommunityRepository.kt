package com.siffmember.info.data.local.repository

import androidx.lifecycle.LiveData
import com.siffmember.info.data.local.dao.CommunityDao
import com.siffmember.info.data.local.entity.CategoryTagEntity
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.ui.model.MembersGroup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class CommunityRepository(private val communityDao: CommunityDao) {

    val allCommunities: Flow<List<CommunityEntity>> = communityDao.getAllCommunities()
    val allTags: Flow<List<String>> = communityDao.getAllTags()

    fun insertCommunity(community: CommunityEntity) {
        CoroutineScope(Dispatchers.IO).launch {
            communityDao.insertCommunity(community)
        }
    }
    suspend fun insertAllCommunity(community: List<CommunityEntity>) {
        return communityDao.insertAllCommunity(community)
    }

    suspend fun updateFilePath(groupID: String, filePath: String) {
        communityDao.updateFilePath(groupID, filePath)
    }

    suspend fun updateGroupMembers(groupId: String, newMembers: List<MembersGroup>) {
        communityDao.updateMembers(groupId, newMembers)
    }

    suspend fun updateGroupStatus(groupId: String, groupStatus: Boolean) {
        communityDao.updateGroupStatus(groupId, groupStatus)
    }

    suspend fun updateGroupDetails(groupId: String, groupName: String, description: String, groupIcon: String) {
        communityDao.updateGroupDetails(groupId, groupName, description, groupIcon)
    }

    suspend fun deleteCommunityByID(groupId: String) {
        communityDao.deleteCommunitiesByGroupID(groupId)
    }

    suspend fun deleteAllCommunity() {
        communityDao.deleteAllCommunities()
    }

    // Category Tag Queries
    suspend fun insertCategoryTag(tag: CategoryTagEntity) {
        communityDao.insertCategoryTag(tag)
    }

    /*fun getTagsForCommunity(communityId: Int): Flow<List<CategoryTagEntity>> {
        return communityDao.getTagsForCommunity(communityId)
    }*/

    fun getCommunitiesByTag(tagName: String): Flow<List<CommunityEntity>> {
        return communityDao.getCommunitiesByTag(tagName)
    }
    suspend fun getCommunitiesById(groupId: String): CommunityEntity? {
        return communityDao.getCommunityById(groupId)
    }

     fun getAllCommunities(): List<CommunityEntity>{
        return communityDao.getAllCommunitiesList()
    }
    fun getAllCommunitiesIdList(): List<String>{
        return communityDao.getAllCommunitiesIdList()
    }

    fun getCommunitiesMemberById(groupId: String): LiveData<CommunityEntity?> {
        return communityDao.getCommunitiesMemberById(groupId)
    }

    suspend fun getTotalCategoryCount(): Int {
        return communityDao.getTotalCategoryCount()
    }

    suspend fun doesCategoryExist(tagName: String, groupId: String): Boolean {
        return communityDao.doesCategoryExist(tagName, groupId) > 0
    }

    suspend fun deleteCategoryTagByName(tagName: String) {
        communityDao.deleteCategoryTagByName(tagName)
    }

    suspend fun deleteAllCategoryTag() {
        communityDao.deleteAllCategoryTag()
    }
}