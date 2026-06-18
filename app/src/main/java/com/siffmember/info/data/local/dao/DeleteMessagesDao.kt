package com.siffmember.info.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.siffmember.info.data.local.entity.DeleteMessages

@Dao
interface DeleteMessagesDao {
   /* @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedMessage(message: DeleteMessages)*/

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeletedMessage(posts: List<DeleteMessages>)

    /*@Query("SELECT * FROM delete_messages ORDER BY timestamp ASC")
    fun getAllDeletedMessages(): LiveData<List<DeleteMessages>>*/

    @Query("SELECT * FROM delete_messages WHERE postId = :postId ORDER BY timestamp DESC LIMIT 1")
    fun getDeleteMessageByPost(postId: String): LiveData<DeleteMessages>

    @Query("SELECT postId FROM delete_messages WHERE postId IN (:postIds)")
    fun getAlreadyInsertedDeletedIds(postIds: List<String>): List<String>

    @Query("DELETE FROM delete_messages WHERE postId = :postId")
    suspend fun deleteDeletedMessageByPostID(postId: String)

}