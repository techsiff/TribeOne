package com.siffmember.info.ui.backup


import android.content.Context
import androidx.work.*
import com.siffmember.info.ui.services.BackupWorker
import java.util.concurrent.TimeUnit

object BackupScheduler {

    fun scheduleWeekly(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(7, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "WEEKLY_BACKUP",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun scheduleMonthly(context: Context) {
        val request = PeriodicWorkRequestBuilder<BackupWorker>(30, TimeUnit.DAYS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "MONTHLY_BACKUP",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelWeekly(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("WEEKLY_BACKUP")
    }

    fun cancelMonthly(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("MONTHLY_BACKUP")
    }
}
