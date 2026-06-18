package com.siffmember.info.application

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.utils.AppConstants
import us.zoom.sdk.MeetingParameter
import us.zoom.sdk.MeetingServiceListener
import us.zoom.sdk.MeetingStatus
import us.zoom.sdk.ZoomSDK

object GlobalMeetingListener : MeetingServiceListener {

    private const val TAG = "GlobalMeetingListener"

    private val db: FirebaseFirestore
        get() = Firebase.firestore
    private var roomName = ""
    private var meetingId = ""
    private var meetingTitle = ""
    private var hostId = ""
    private var hostName = ""

    private var meetingStartTimestamp = ""

    // Capture actual host status when leaving
    private var wasHostWhenLeaving = false
    private var isReconnectingMeeting = false
    private var currentSessionId: String? = null

    private var isParticipantInsideMeeting = false
    private var isMeetingStartedByHost = false
    private var hasProcessedLeave = false

    fun initialize(
        roomName: String,
        meetingId: String,
        meetingTitle: String,
        hostId: String,
        hostName: String,
        meetingStartTimestamp: String
    ) {
        this.roomName = roomName
        this.meetingId = meetingId
        this.meetingTitle = meetingTitle
        this.hostId = hostId
        this.hostName = hostName
        this.meetingStartTimestamp = meetingStartTimestamp
    }

    override fun onMeetingParameterNotification(meetingParameter: MeetingParameter?) {
        Log.e(TAG, "Meeting Parameter: $meetingParameter")
    }

    override fun onMeetingStatusChanged(meetingStatus: MeetingStatus?, errorCode: Int, internalErrorCode: Int) {
        Log.e(TAG, "Meeting Status=$meetingStatus error=$errorCode internal=$internalErrorCode")
        when (meetingStatus) {
            MeetingStatus.MEETING_STATUS_INMEETING -> {
                hasProcessedLeave = false
                val currentTime = System.currentTimeMillis().toString()
                val isCurrentHost =
                    ZoomSDK.getInstance()
                        .meetingService
                        ?.isCurrentMeetingHost ?: false
                Log.e(TAG, "INMEETING host=$isCurrentHost")
                if (isCurrentHost && !isMeetingStartedByHost) {
                    isMeetingStartedByHost = true
                    meetingStarted(currentTime)
                }
                if (!isParticipantInsideMeeting) {
                    isParticipantInsideMeeting = true
                    participantJoined(currentTime)
                }
            }
            MeetingStatus.MEETING_STATUS_RECONNECTING -> {
                isReconnectingMeeting = true
                Log.e(TAG, "RECONNECTING")
            }
            MeetingStatus.MEETING_STATUS_DISCONNECTING -> {
                wasHostWhenLeaving = ZoomSDK.getInstance().meetingService?.isCurrentMeetingHost ?: false
                Log.e(TAG, "DISCONNECTING host=$wasHostWhenLeaving")
            }
            MeetingStatus.MEETING_STATUS_FAILED -> {
                handleMeetingExit("FAILED")
            }
            MeetingStatus.MEETING_STATUS_ENDED -> {
                handleMeetingExit("ENDED")
            }
            MeetingStatus.MEETING_STATUS_IDLE -> {
                handleMeetingExit("IDLE")
            }
            else -> {}
        }
    }

    private fun handleMeetingExit(reason: String) {
        if (hasProcessedLeave) {
            return
        }
        hasProcessedLeave = true
        Log.e(TAG, "handleMeetingExit : $reason")
        val currentTime = System.currentTimeMillis().toString()
        if (isParticipantInsideMeeting) {
            participantLeft(currentTime)
            isParticipantInsideMeeting = false
        }
        if (wasHostWhenLeaving || isMeetingStartedByHost) {
            meetingEnded(currentTime)
        }
        isMeetingStartedByHost = false
        isReconnectingMeeting = false
    }

    private fun meetingStarted(currentTime: String) {
        try {
            meetingStartTimestamp = currentTime
            val meetingRef = db
                .collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                .document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                .document(meetingId)

            meetingRef.update(mapOf(
                    "meetingStartTimestamp" to currentTime,
                    "inMeeting" to true
                )
            )
                .addOnSuccessListener {
                    Log.e(TAG, "meetingStarted update SUCCESS")
                }
                .addOnFailureListener {
                    Log.e(TAG, "meetingStarted update ERROR", it)
                }
            val analyticsRef = meetingRef
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
                .document(currentTime)
            val analyticsData = hashMapOf(
                "hostName" to hostName,
                "hostNumber" to hostId,
                "meetingNumber" to meetingId,
                "topicName" to meetingTitle,
                "startTime" to currentTime
            )
            analyticsRef
                .set(analyticsData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.e(TAG, "analytics document created SUCCESS")
                }
                .addOnFailureListener {
                    Log.e(TAG, "analytics document created ERROR", it)
                }
        } catch (e: Exception) {
            Log.e(TAG, "meetingStarted exception", e)
        }
    }

    private fun meetingEnded(currentTime: String) {
        try {
            val meetingRef = db
                .collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                .document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                .document(meetingId)

            meetingRef.update(mapOf(
                    "meetingStartTimestamp" to "",
                    "inMeeting" to false
                )
            )
                .addOnSuccessListener {
                    Log.e(TAG, "meetingEnded update SUCCESS")
                }
                .addOnFailureListener {
                    Log.e(TAG, "meetingEnded update ERROR", it)
                }

            if (meetingStartTimestamp.isNotEmpty()) {
                meetingRef
                    .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
                    .document(meetingStartTimestamp)
                    .update("endTime", currentTime)
                    .addOnSuccessListener {
                        Log.e(TAG, "analytics endTime SUCCESS")
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "analytics endTime ERROR", it)
                    }
            }
        } catch (e: Exception) {
            Log.e(TAG, "meetingEnded exception", e)
        }
    }

    private fun participantJoined(currentTime: String) {
        try {
            if (meetingStartTimestamp.isEmpty()) {
                return
            }
            if (currentSessionId != null) {
                Log.e(TAG, "participantJoined skipped session already active")
                return
            }
            val participantRef =
                db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                    .document(roomName)
                    .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                    .document(meetingId)
                    .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
                    .document(meetingStartTimestamp)
                    .collection("participants")
                    .document(hostId)

            participantRef.set(
                mapOf("userId" to hostId,
                    "userName" to hostName
                ),
                SetOptions.merge()
            )

            val sessionRef = participantRef.collection("sessions").document()
            currentSessionId = sessionRef.id
            sessionRef.set(mapOf("joinTime" to currentTime))
                .addOnSuccessListener {
                    Log.e(TAG, "participantJoined SUCCESS session=$currentSessionId")
                }
                .addOnFailureListener {
                    currentSessionId = null
                    Log.e(TAG, "participantJoined ERROR", it)
                }
        } catch (e: Exception) {
            Log.e(TAG, "participantJoined exception", e)
        }
    }

    private fun participantLeft(currentTime: String) {
        try {
            val sessionId = currentSessionId
            if (sessionId == null) {
                Log.e(TAG, "participantLeft skipped currentSessionId null")
                return
            }
            db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                .document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                .document(meetingId)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
                .document(meetingStartTimestamp)
                .collection("participants")
                .document(hostId)
                .collection("sessions")
                .document(sessionId)
                .set(mapOf(
                        "leaveTime" to currentTime
                    ),
                    SetOptions.merge()
                )
                .addOnSuccessListener {
                    currentSessionId = null
                    Log.e(TAG, "participantLeft SUCCESS")
                }
                .addOnFailureListener {
                    Log.e(TAG, "participantLeft ERROR", it)
                }
        } catch (e: Exception) {
            Log.e(TAG, "participantLeft exception", e)
        }
    }
}