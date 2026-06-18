package com.siffmember.info.ui.backup

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import com.siffmember.info.data.local.database.AppDatabase
import com.siffmember.info.data.local.database.PostMessageDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.zip.ZipInputStream

class DatabaseBackupManager(private val context: Context) {

    private val db1 = context.getDatabasePath("siffmember_database")
    private val db2 = context.getDatabasePath("posts_database")

    private fun tmpDir(): File = context.cacheDir

    /* -----------------------------------------------------------
                        ENCRYPTED ZIP BACKUP
    ------------------------------------------------------------*/
    /*private fun getTime(): String {

        val currentTime = Calendar.getInstance().time

        val sdf =
            if (Build.VERSION.SDK_INT <= 28) {
                SimpleDateFormat("yyyy-MM-dd-HH_mm_ss", Locale.getDefault())
            } else {
                SimpleDateFormat("yyyy-MM-dd-HH:mm:ss", Locale.getDefault())
            }

        return sdf.format(currentTime)
    }*/
    /** ZIP → Encrypt → Save as .enc */
    suspend fun backupEncryptedZipToFolder(folderUri: Uri) = withContext(Dispatchers.IO) {
        // STEP 1: ZIP files
        val zipFile = File(tmpDir(), "tribeOneBackup.zip")

        ZipUtils.zipFiles(
            listOf(
                Pair("siffmember_database.db", db1)

            ), zipFile
        )
       // Pair("posts_database.db", db2)
        // STEP 2: Encrypt
        val encFile = File(tmpDir(), "tribeOneBackup.enc")
        CryptoUtils.encryptFile(context, zipFile, encFile)

        // STEP 3: Export
        val createdUri = DocumentsContract.createDocument(
            context.contentResolver,
            folderUri,
            "application/octet-stream",
            "tribeOneBackup.enc"
        ) ?: throw IllegalStateException("Cannot create backup file")

        context.contentResolver.openOutputStream(createdUri)!!.use { out ->
            FileInputStream(encFile).use { it.copyTo(out) }
        }

        zipFile.delete()
        encFile.delete()
    }


    /* -----------------------------------------------------------
                 RESTORE ENCRYPTED ZIP (.enc) BACKUP
    ------------------------------------------------------------*/

    suspend fun restoreEncryptedZipFromFolder(folderUri: Uri, encFileName: String) =
        withContext(Dispatchers.IO) {

            closeRoomDatabases()  // VERY IMPORTANT

            // 1️⃣ Find .enc file
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                folderUri,
                DocumentsContract.getDocumentId(folderUri)
            )

            val cursor = context.contentResolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME
                ),
                null, null, null
            ) ?: throw IllegalStateException("Cannot read folder")

            var docId: String? = null
            cursor.use {
                while (it.moveToNext()) {
                    if (it.getString(1) == encFileName) {
                        docId = it.getString(0)
                        break
                    }
                }
            }

            docId ?: throw IllegalArgumentException("Backup file not found")

            // 2️⃣ Copy .enc to temp
            val encUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, docId)
            val encTmp = File(tmpDir(), "tribeOneBackup.enc")
            context.contentResolver.openInputStream(encUri)!!.use { input ->
                FileOutputStream(encTmp).use { output -> input.copyTo(output) }
            }

            // 3️⃣ Decrypt → ZIP
            val zipTmp = File(tmpDir(), "tribeOneBackup.zip")
            CryptoUtils.decryptFile(context, encTmp, zipTmp)

            // 4️⃣ Unzip and replace actual DB files
            unzipAndReplaceDatabases(zipTmp)

            reopenRoomDatabases(context)
            encTmp.delete()
            zipTmp.delete()
        }


    /* -----------------------------------------------------------
                       INTERNAL UTILITIES
    ------------------------------------------------------------*/

    private fun closeRoomDatabases() {
        try {
            AppDatabase::class.java.getDeclaredField("instance").apply {
            isAccessible = true
            (get(null) as? androidx.room.RoomDatabase)?.close()
            set(null, null)
        }} catch (_: Exception) {}

        try {
            PostMessageDatabase::class.java.getDeclaredField("instance").apply {
            isAccessible = true
            (get(null) as? androidx.room.RoomDatabase)?.close()
            set(null, null)
        }} catch (_: Exception) {}
    }

    fun reopenRoomDatabases(context: Context) {
        try {
            AppDatabase.getDatabase(context)
        } catch (_: Exception) {}

        try {
           // PostMessageDatabase.getDatabase(context)
        } catch (_: Exception) {}
    }

    private fun unzipAndReplaceDatabases(zipFile: File) {
        val zip = ZipInputStream(FileInputStream(zipFile))
        val buffer = ByteArray(8192)

        var entry = zip.nextEntry
        while (entry != null) {

            val target = when (entry.name) {
                "siffmember_database.db" -> db1
              //  "posts_database.db" -> db2
                else -> null
            }

            if (target != null) {
                val tmp = File(target.parent, target.name + ".tmp")

                FileOutputStream(tmp).use { fos ->
                    var len: Int
                    while (zip.read(buffer).also { len = it } > 0) {
                        fos.write(buffer, 0, len)
                    }
                }

                if (target.exists()) target.delete()
                tmp.renameTo(target)
            }

            zip.closeEntry()
            entry = zip.nextEntry
        }

        zip.close()
    }
}
