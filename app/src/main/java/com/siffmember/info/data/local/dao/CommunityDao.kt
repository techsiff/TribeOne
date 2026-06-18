package com.siffmember.info.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.siffmember.info.data.local.entity.CategoryTagEntity
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.ui.model.MembersGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityDao {
    // Community Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunity(community: CommunityEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCommunity(posts: List<CommunityEntity>)

    @Query("SELECT * FROM communities ORDER BY createdAt DESC")
    fun getAllCommunities(): Flow<List<CommunityEntity>>

    @Query("SELECT * FROM communities WHERE groupID = :groupID LIMIT 1")
    suspend fun getCommunityById(groupID: String): CommunityEntity?

    @Query("SELECT * FROM communities WHERE groupID = :groupID LIMIT 1")
    fun getCommunitiesMemberById(groupID: String): LiveData<CommunityEntity?>

    @Query("SELECT * FROM communities")
    fun getAllCommunitiesList(): List<CommunityEntity>

    @Query("SELECT groupID FROM communities")
    fun getAllCommunitiesIdList(): List<String>

    @Query("UPDATE communities SET groupIcon = :filePath WHERE groupID = :groupID")
    suspend fun updateFilePath(groupID: String, filePath: String)

    @Query("UPDATE communities SET members = :members WHERE groupID = :groupId")
    suspend fun updateMembers(groupId: String, members: List<MembersGroup>)

    @Query("UPDATE communities SET groupStatus = :groupStatus WHERE groupID = :groupId")
    suspend fun updateGroupStatus(groupId: String, groupStatus: Boolean)

    @Query("UPDATE communities SET groupName = :groupName, description = :description, groupIcon = :groupIcon WHERE groupID = :groupId")
    suspend fun updateGroupDetails(groupId: String, groupName: String, description: String, groupIcon: String)

    @Query("DELETE FROM communities")
    suspend fun deleteAllCommunities()

    @Query("DELETE FROM communities WHERE groupId = :groupId")
    suspend fun deleteCommunitiesByGroupID(groupId: String)

    // Category Tag Queries
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategoryTag(tag: CategoryTagEntity)

   /* @Query("SELECT * FROM category_tags WHERE communityId = :communityId")
    fun getTagsForCommunity(communityId: Int): Flow<List<CategoryTagEntity>>*/

    @Query("SELECT DISTINCT tagName FROM category_tags")
    fun getAllTags(): Flow<List<String>>

    @Query("SELECT communities.* FROM communities INNER JOIN category_tags ON communities.groupID = category_tags.communityId WHERE category_tags.tagName = :tagName")
    fun getCommunitiesByTag(tagName: String): Flow<List<CommunityEntity>>

    @Query("SELECT COUNT(DISTINCT tagName) FROM category_tags")
    suspend fun getTotalCategoryCount(): Int

    @Query("SELECT COUNT(*) FROM category_tags WHERE tagName = :tagName AND communityId = :groupId")
    suspend fun doesCategoryExist(tagName: String, groupId: String): Int

    @Query("DELETE FROM category_tags WHERE tagName = :tagName")
    suspend fun deleteCategoryTagByName(tagName: String)

    @Query("DELETE FROM category_tags")
    suspend fun deleteAllCategoryTag()
}