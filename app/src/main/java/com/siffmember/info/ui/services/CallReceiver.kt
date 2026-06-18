package com.siffmember.info.ui.services

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.telephony.TelephonyManager
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.data.local.repository.PostMessageRepository
import com.siffmember.info.ui.model.UpdateCallLog
import com.siffmember.info.ui.model.UpdateUsersCallLog
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CallLogDetails
import com.siffmember.info.utils.OpenPoints

class CallReceiver : BroadcastReceiver() {

    companion object {
        var TAG = "CallReceiver"

        //private var isCallRunning = false
        var lastState: String = TelephonyManager.EXTRA_STATE_IDLE
        var incomingNumber: String = ""
        var isIncoming = false
        var isOutgoing = false
        var ringStartTime = 0L
        var callStartTime = 0L
    }

    private lateinit var postRepository: PostMessageRepository
    lateinit var sharedPref: SharedPreferences
    private var userId = ""
    private var userName = ""
    private lateinit var db: FirebaseFirestore

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        db = Firebase.firestore
        sharedPref = context.getSharedPreferences(AppConstants.PREF_NAME, MODE_PRIVATE)
        postRepository = PostMessageRepository(context)

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE) ?: return

        when (stateStr) {
            // INCOMING RINGING
            TelephonyManager.EXTRA_STATE_RINGING -> {
                Log.e(TAG, "📞 Incoming RINGING from: $incomingNumber")
                isIncoming = true
                isOutgoing = false
            }
            // OFFHOOK (Incoming Answered OR Outgoing Started)
            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                if (lastState == TelephonyManager.EXTRA_STATE_RINGING) {
                    // Incoming Answered
                    Log.e(TAG, "✅ INCOMING CALL ANSWERED: $incomingNumber")
                    isIncoming = true
                    isOutgoing = false
                } else {
                    // Outgoing call
                    Log.e(TAG, "📤 OUTGOING CALL STARTED")
                    isOutgoing = true
                    isIncoming = false
                }
                callStartTime = System.currentTimeMillis()
            }
            // IDLE (Call finished / missed / declined)
            TelephonyManager.EXTRA_STATE_IDLE -> {
                // End of Outgoing or Answered Call
                if (lastState == TelephonyManager.EXTRA_STATE_OFFHOOK) {
                    val duration = (System.currentTimeMillis() - callStartTime) / 1000
                    if (isOutgoing) {
                        Log.e(TAG, "📤 OUTGOING CALL ENDED. Duration: ${formatDuration(duration)}")
                    } else {
                        Log.e(TAG, "📵 INCOMING CALL ENDED. Duration: ${formatDuration(duration)}")
                    }
                    if (CallLogDetails.getCallInitiated()) {
                        val timeStamp = System.currentTimeMillis().toString()
                        if(OpenPoints.getIsGuestUser()){
                            userId = CallLogDetails.getGuestUserPhoneNumber()
                            userName = CallLogDetails.getGuestUserName()
                        } else {
                            userId = sharedPref.getString(AppConstants.USER_ID, "").toString()
                            userName = sharedPref.getString(AppConstants.USER_NAME, "").toString()
                        }
                        updateUsersCallLog(
                            UpdateUsersCallLog(
                            userName,
                            userId,
                            CallLogDetails.getUserName(),
                            CallLogDetails.getUserPhoneNumber(),
                            duration.toString(),
                            timeStamp
                            )
                        )
                        /*updateIncomingCallLog(
                            CallLogDetails.getUserPhoneNumber(),
                            UpdateCallLog(
                                userName,
                                userId,
                                "1",
                                duration.toString(),
                                timeStamp
                            )
                        )
                        updateOutGoingCallLog(
                            userId, UpdateCallLog(
                                CallLogDetails.getUserName(),
                                CallLogDetails.getUserPhoneNumber(),
                                "2",
                                duration.toString(),
                                timeStamp
                            )
                        )*/
                        CallLogDetails.setCallInitiated(false)
                        CallLogDetails.setUserName("")
                        CallLogDetails.setUserPhoneNumber("")
                        CallLogDetails.setGuestUserName("")
                        CallLogDetails.setGuestUserPhoneNumber("")
                    }
                }
                // RINGING → IDLE (Missed or Declined)
                if (lastState == TelephonyManager.EXTRA_STATE_RINGING) {
                    val ringTime = System.currentTimeMillis() - ringStartTime
                    if (ringTime < 2000) {
                        Log.e(TAG, "❌ CALL DECLINED (Rejected) : $incomingNumber")
                    } else {
                        Log.e(TAG, "⛔ MISSED CALL: $incomingNumber")
                    }
                }
                isOutgoing = false
                isIncoming = false
            }
        }

        lastState = stateStr
    }

   /* private fun startServiceOnce() {
        if (!isCallRunning) {
            isCallRunning = true
            Log.e(TAG, "Service Started Once")
        }
    }

    private fun stopService(context: Context) {
        Log.e(TAG, "Service Stopped")
        //val lastCall = getLastCall(context)
        //Log.e(TAG, "getLastCall ${lastCall!!.number}:: Type ${lastCall.type}:: Duration ${lastCall.duration}:: Date ${lastCall.timestamp}")
    }

    fun getNationalWithoutCountryCode(raw: String): String {
        return try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val numberProto = phoneUtil.parse(raw, null) // auto-detect region
            numberProto.nationalNumber.toString()
        } catch (_: Exception) {
            raw
        }
    }*/

    private fun formatDuration(sec: Long): String {
        val m = sec / 60
        val s = sec % 60
        return "${m}m ${s}s"
    }

    private fun updateOutGoingCallLog(userId: String, callLog: UpdateCallLog){
        try{
            val docRef = db.collection(AppConstants.TABLE_CALL_HISTORY_DETAILS).document(userId)
            docRef.set(mapOf("placeholder" to true), SetOptions.merge())
            val userRef = docRef.collection(AppConstants.TABLE_HISTORY).document()
            userRef.set(callLog)
                .addOnSuccessListener {
                    Log.e(TAG, "DocumentSnapshot successfully written!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun updateIncomingCallLog(userId: String, callLog: UpdateCallLog){
        try{
            val docRef = db.collection(AppConstants.TABLE_CALL_HISTORY_DETAILS).document(userId)
            docRef.set(mapOf("placeholder" to true), SetOptions.merge())
            val userRef = docRef.collection(AppConstants.TABLE_HISTORY).document()
            userRef.set(callLog)
                .addOnSuccessListener {
                    Log.e(TAG, "DocumentSnapshot successfully written!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun updateUsersCallLog(callLog: UpdateUsersCallLog){
        try{
            val docRef = db.collection(AppConstants.TABLE_USERS_CALL_HISTORY_DETAILS).document()
            docRef.set(callLog)
                .addOnSuccessListener {
                    Log.e(TAG, "DocumentSnapshot successfully written!")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
}
