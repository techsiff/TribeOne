package com.siffmember.info.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.siffmember.info.data.local.dao.CommunityDao
import com.siffmember.info.data.local.dao.DeleteMessagesDao
import com.siffmember.info.data.local.dao.PostMessageDao
import com.siffmember.info.data.local.dao.ReplyPostMessageDao
import com.siffmember.info.data.local.dao.UserDetailsDao
import com.siffmember.info.data.local.entity.CategoryTagEntity
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.data.local.entity.DeleteMessages
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.data.local.entity.UserDetailsEntity
import com.siffmember.info.ui.backup.PassPhraseUtils
import com.siffmember.info.ui.backup.SQLCipherUtils
import net.sqlcipher.database.SupportFactory

@Database(
    entities = [
        CommunityEntity::class,
        CategoryTagEntity::class,
        PostMessage::class,
        ReplyPostMessage::class,
        DeleteMessages::class,
        UserDetailsEntity::class
               ],
    version = 5,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDetailsDao
    abstract fun communityDao(): CommunityDao
    abstract fun postDao(): PostMessageDao
    abstract fun replyDao(): ReplyPostMessageDao
    abstract fun deletedMessageDao(): DeleteMessagesDao

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DATABASE_NAME = "siffmember_database"

        fun getDatabase(context: Context): AppDatabase {

            return INSTANCE ?: synchronized(this) {

                val userPassphrase = PassPhraseUtils.getPassphrase(context)
                val passphraseBytes = userPassphrase.toByteArray()
                // SQLCipher SupportFactory
                val factory = SupportFactory(passphraseBytes)
                // ✔ Check encryption state BEFORE Room is opened
                val state = SQLCipherUtils.getDatabaseState(context, DATABASE_NAME)
                if (state == SQLCipherUtils.State.UNENCRYPTED) {
                    SQLCipherUtils.migrateToEncryptedDatabase(
                        DATABASE_NAME,
                        context,
                        userPassphrase
                    )
                }
                // ✔ Build encrypted Room database
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(Migrations.MIGRATION_2_3, Migrations.MIGRATION_3_4, Migrations.MIGRATION_4_5)
                    .openHelperFactory(factory)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
