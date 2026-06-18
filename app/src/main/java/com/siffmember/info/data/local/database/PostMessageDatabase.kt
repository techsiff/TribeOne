package com.siffmember.info.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.siffmember.info.data.local.dao.PostMessageDao
import com.siffmember.info.data.local.dao.ReplyPostMessageDao
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.ui.backup.PassPhraseUtils
import com.siffmember.info.ui.backup.SQLCipherUtils
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [PostMessage::class, ReplyPostMessage::class],
    version = 3,
    exportSchema = false
)
abstract class PostMessageDatabase : RoomDatabase() {

    abstract fun postsDao(): PostMessageDao
    abstract fun postsReplyDao(): ReplyPostMessageDao

    companion object {

        private var instance: PostMessageDatabase? = null
        private const val DATABASE_NAME = "posts_database"

        fun getDatabase(context: Context): PostMessageDatabase {
            // 1) get passphrase
            val userPassphrase = PassPhraseUtils.getPassphrase(context)
            val passphrase = userPassphrase.toByteArray()
            // 2) check encryption state
            val state = SQLCipherUtils.getDatabaseState(context, DATABASE_NAME)
            // 3) SQLCipher factory
            val factory = SupportFactory(passphrase)
            // 4) migrate only once if unencrypted
            if (state == SQLCipherUtils.State.UNENCRYPTED) {
                SQLCipherUtils.migrateToEncryptedDatabase(
                    DATABASE_NAME,
                    context,
                    userPassphrase
                )
            }
            // 5) build database once (singleton)
            if (instance == null) {
                instance = Room.databaseBuilder(
                    context.applicationContext,
                    PostMessageDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(
                        Migrations.MIGRATION_POST_1_2,
                        Migrations.MIGRATION_POST_2_3
                    )
                    .openHelperFactory(factory)
                    .build()
            }

            return instance!!
        }
    }
}
