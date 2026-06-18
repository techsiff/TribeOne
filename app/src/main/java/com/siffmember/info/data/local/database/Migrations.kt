package com.siffmember.info.data.local.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase


object Migrations {

    val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Step 1: Create new table
            db.execSQL("""
            CREATE TABLE category_tags_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                communityId TEXT NOT NULL,
                tagName TEXT NOT NULL
            )
        """.trimIndent())
            // Step 2: Copy the data, casting `communityId` to TEXT
            db.execSQL("""
            INSERT INTO category_tags_new (id, communityId, tagName)
            SELECT id, CAST(communityId AS TEXT), tagName FROM category_tags
        """.trimIndent())
            // Step 3: Drop the old table
            db.execSQL("DROP TABLE category_tags")
            // Step 4: Rename new table to the original name
            db.execSQL("ALTER TABLE category_tags_new RENAME TO category_tags")
        }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS post_messages (
                    postId TEXT NOT NULL PRIMARY KEY,
                    postTitle TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    groupName TEXT NOT NULL,
                    groupId TEXT NOT NULL,
                    userName TEXT NOT NULL,
                    userId TEXT NOT NULL
                )
                """
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS replyPost_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    commentId TEXT NOT NULL,
                    postId TEXT NOT NULL,
                    postTitle TEXT NOT NULL,
                    content TEXT NOT NULL,
                    timestamp TEXT NOT NULL,
                    groupId TEXT NOT NULL,
                    userName TEXT NOT NULL,
                    userId TEXT NOT NULL
                )
                """
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS delete_messages (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    postId TEXT NOT NULL,
                    timestamp TEXT NOT NULL
                )
                """
            )
        }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(db: SupportSQLiteDatabase) {

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_details (
                    phone_number TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    email_id TEXT NOT NULL,
                    country TEXT NOT NULL,
                    category TEXT NOT NULL
                )
                """
            )

        }
    }

    val MIGRATION_POST_1_2: Migration = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Define the migration logic, e.g., adding a new table or column
            db.execSQL("ALTER TABLE replyPost_messages ADD COLUMN commentId TEXT NOT NULL DEFAULT '0'")
        }
    }

    val MIGRATION_POST_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // Define the migration logic, e.g., adding a new table or column
            db.execSQL("ALTER TABLE replyPost_messages ADD COLUMN groupId TEXT NOT NULL DEFAULT '0'")
        }
    }
}