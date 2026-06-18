package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.siffmember.info.R
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@SuppressLint("CustomSplashScreen")
class SplashScreenActivity : BaseActivity() {
    companion object {
        var TAG = "SplashScreenActivity"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        FirebaseMessaging.getInstance().isAutoInitEnabled = true
        val handler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
        val runnable = Runnable {
            if(sharedPref.getBoolean(AppConstants.IS_LOGGEDIN, false)){
                val nextIntent = Intent(this@SplashScreenActivity, HomeActivity::class.java)
                startActivity(nextIntent)
                finish()
            } else {
                val nextIntent = Intent(this@SplashScreenActivity, IntroActivity::class.java)
                startActivity(nextIntent)
                finish()
            }
        }
        handler.schedule(runnable, 1, TimeUnit.SECONDS)
        registerTopics(AppConstants.ANNOUNCEMENT_TOPIC)
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e(TAG, "Fetching FCM token failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result
                Log.e(TAG, "FCM Token: $token")
                CommunityChat.setFCMToken(token)
            }
    }
}