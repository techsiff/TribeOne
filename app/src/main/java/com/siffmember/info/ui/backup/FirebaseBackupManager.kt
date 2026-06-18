/*
package com.siffmember.info.ui.backup

import android.content.Context
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class FirebaseBackupManager(private val context: Context) {

    private val storage = FirebaseStorage.getInstance()

    */
/** Upload DB file *//*

    suspend fun uploadBackup(file: File, remoteName: String) {
        val ref = storage.reference.child("backups/$remoteName")
        val stream = FileInputStream(file)
        ref.putStream(stream).await()
    }

    */
/** Download DB file *//*

    suspend fun downloadBackup(remoteName: String, dest: File) {
        val ref = storage.reference.child("backups/$remoteName")
        val bytes = ref.getBytes(50 * 1024 * 1024).await() // 50MB max
        FileOutputStream(dest).use { it.write(bytes) }
    }
}
*/
