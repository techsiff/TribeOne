package com.siffmember.info.ui.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.siffmember.info.ui.backup.DatabaseBackupManager
import androidx.core.net.toUri

class BackupWorker(private val context: Context, workerParams: WorkerParameters)
    : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val folder = inputData.getString("backupFolderUri") ?: return Result.failure()
        val folderUri = folder.toUri()

        val manager = DatabaseBackupManager(context)

        return try {
           // manager.createWeeklyBackup(folderUri)
            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}
