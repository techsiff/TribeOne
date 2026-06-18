package com.siffmember.info.data.local.database

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object DatabaseMerger {

    private const val PREF_NAME = "db_merge_pref"
    private const val KEY_MERGE_DONE = "merge_completed"

    suspend fun autoMergeIfNeeded(context: Context) = withContext(Dispatchers.IO) {
        try{
            Log.e("DatabaseMerger", "autoMergeIfNeeded")
            val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            if (pref.getBoolean(KEY_MERGE_DONE, false)) {
                Log.e("DatabaseMerger", "Already Merged")
                return@withContext
            }
            val mainDb = AppDatabase.getDatabase(context)
            val postDb = PostMessageDatabase.getDatabase(context)
            val posts = postDb.postsDao().getAllPostsMessages()
            val replies = postDb.postsReplyDao().getAllReplyPostsMessages()
            Log.e("DatabaseMerger", "POSTS = ${posts.size}  | REPLIES = ${replies.size}")
            // Run inside Room transaction (SUSPEND version)
            mainDb.withTransaction {
                mainDb.postDao().insertAllPosts(posts)
                mainDb.replyDao().insertAllReplies(replies)
            }
            // Close old database and delete
            postDb.close()
            val deleted = context.deleteDatabase("posts_database")
            Log.e("DatabaseMerger", "DB deleted = $deleted")
            pref.edit().putBoolean(KEY_MERGE_DONE, true).apply()
            Log.e("DatabaseMerger", "Merge COMPLETED")
        } catch (e: Exception){
            e.printStackTrace()
        }

    }
}
