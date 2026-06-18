package com.siffmember.info.ui.services

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.siffmember.info.R
import com.siffmember.info.ui.activity.HomeActivity
import com.siffmember.info.ui.viewmodel.NotificationEventBus
import com.siffmember.info.utils.AppConstants
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SIFFMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "SIFFMessagingService"
    }

    private var topicsFrom = "/topics/SIFFAnnouncement"
    lateinit var sharedPref: SharedPreferences

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "onCreate")
        sharedPref = getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)

    }
    @OptIn(DelicateCoroutinesApi::class)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        try {
            //Log.e(TAG, "From: ${remoteMessage.from}")
            topicsFrom = remoteMessage.from!!
            if (remoteMessage.data.isNotEmpty()) {
                //Log.e(TAG, "Message data payload: ${remoteMessage.data}")
                val senderId = remoteMessage.data["senderId"]
                val currentUserId = getStoredUserId()
                val senderName = remoteMessage.data["senderName"] ?: ""
                val notificationType = remoteMessage.data["community_notification"] ?: ""
                val groupId = remoteMessage.data["groupId"] ?: ""
                val groupName = remoteMessage.data["groupName"] ?: ""
                val postId = remoteMessage.data["postId"]  ?: ""
                val postTitle = remoteMessage.data["postTitle"]  ?: ""
                val postContent = remoteMessage.data["postContent"]  ?: ""
                val commentID = remoteMessage.data["commentId"]  ?: ""
                val commentContent = remoteMessage.data["commentContent"]  ?: ""
                val timestamp = remoteMessage.data["timestamp"]  ?: ""

                Log.e(TAG, "notificationType: $notificationType GroupId: $groupId postID: $postId commentID: $commentID")
                Log.e(TAG, "commentContent: $commentContent")

                val inputData = Data.Builder()
                    .putString("notificationType", notificationType)
                    .putString("groupId", groupId)
                    .putString("groupName", groupName)
                    .putString("senderName", senderName)
                    .putString("senderId", senderId)
                    .putString("postId", postId)
                    .putString("postTitle", postTitle)
                    .putString("postContent", postContent)
                    .putString("commentId", commentID)
                    .putString("commentContent", commentContent)
                    .putString("timestamp", timestamp)
                    .build()

                // Constraints: Device must be connected to network
                val constraints = Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
                val workRequest = OneTimeWorkRequestBuilder<TribeOneWorker>()
                    .setConstraints(constraints)
                    .setInputData(inputData)
                    .build()
                WorkManager.getInstance(applicationContext).enqueue(workRequest)

                if (senderId == currentUserId) {
                    //Log.e(TAG, "Skipping notification: Sender and receiver are the same. $senderId")
                    return  // Skip notification if the sender is the current user
                }

                remoteMessage.notification?.let {
                    if (topicsFrom == "/topics/SIFFAnnouncement") {
                        showNotification(it.title ?: "Notification", it.body ?: "No content")
                        GlobalScope.launch {
                            NotificationEventBus.sendEvent("New Announcement")
                        }
                    } else {
                        showCommunityNotification(it.title ?: "Notification", it.body ?: "No content")
                    }
                }
            }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun getStoredUserId(): String? {
        val sharedPreferences = getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
        return sharedPreferences.getString(AppConstants.USER_ID, null)
    }

    override fun onNewToken(token: String) {
        Log.e(TAG, "Refreshed token: $token")
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun showNotification(title: String, message: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val requestCode = 0
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = "siff_member_announcement"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // this expands the message
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SIFF Announcement",
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 0
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun showCommunityNotification(title: String, message: String) {
        val intent = Intent(this, HomeActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val requestCode = 0
        val pendingIntent = PendingIntent.getActivity(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = "siff_member_community"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // this expands the message
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Since android Oreo notification channel is needed.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "SIFF Community",
                NotificationManager.IMPORTANCE_HIGH,
            )
            channel.setShowBadge(true)
            notificationManager.createNotificationChannel(channel)
        }

        val notificationId = 1
        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}