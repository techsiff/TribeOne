package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import com.siffmember.info.ui.activity.HomeActivity.Companion.TAG
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.utils.AppConstants

open class BaseActivity : AppCompatActivity() {

    private var progressDialog: ProgressDialog? = null
    lateinit var sharedPref: SharedPreferences
    lateinit var sharedPrefEditor: SharedPreferences.Editor

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        sharedPref = getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
        sharedPrefEditor = sharedPref.edit()
        progressDialog = ProgressDialog(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            progressDialog!!.setTheme(ProgressDialog.THEME_FOLLOW_SYSTEM)
        }
    }

    /**
     * Showing progress dialog
     */
    fun showProgDialog() {
        try {
            progressDialog!!.setMode(ProgressDialog.MODE_INDETERMINATE)
            progressDialog!!.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dismiss progress dialog
     */
    fun dismissProgDialog() {
        try{
            if(progressDialog != null){
                progressDialog!!.dismiss()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun registerTopics(topic: String){
        try {
            Firebase.messaging.subscribeToTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "$topic Subscribed"
                    if (!task.isSuccessful) {
                        msg = "$topic Subscribe failed"
                    }
                    Log.e(TAG, msg)
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, error.message!!)
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun unregisterTopics(topic: String){
        try {
            Firebase.messaging.unsubscribeFromTopic(topic)
                .addOnCompleteListener { task ->
                    var msg = "$topic UN Subscribed"
                    if (!task.isSuccessful) {
                        msg = "$topic Un Subscribe failed"
                    }
                    Log.e(TAG, msg)
                }
                .addOnFailureListener { error ->
                    Log.e(TAG, error.message!!)
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
}