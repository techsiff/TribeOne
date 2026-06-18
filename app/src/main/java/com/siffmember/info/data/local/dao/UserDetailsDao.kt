package com.siffmember.info.data.local.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.siffmember.info.data.local.entity.UserDetailsEntity

@Dao
interface UserDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserDetails(users: UserDetailsEntity)

    @Query("SELECT * FROM user_details WHERE phone_number = :userId")
    fun getUserDetails(userId: String): LiveData<UserDetailsEntity?>

    @Query("DELETE FROM user_details")
    suspend fun deleteUserDetails()
}