package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityMeetingAnalyticsBinding
import com.siffmember.info.ui.adapter.MeetingsAnalyticsAdapter
import com.siffmember.info.ui.model.MeetingAnalyticsModel
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.ui.model.ParticipantModel
import com.siffmember.info.ui.model.SessionModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.MeetingUserDetails

class MeetingsAnalyticsActivity : BaseActivity(), MeetingsAnalyticsAdapter.MeetingsListener {

    companion object {
        var TAG = "MeetingsAnalyticsActivity"
    }
    private lateinit var binding: ActivityMeetingAnalyticsBinding
    private lateinit var db: FirebaseFirestore
    private var meetingsList: ArrayList<MeetingAnalyticsModel> = ArrayList()
    private var recyclerViewAdapter: MeetingsAnalyticsAdapter? = null
    private var hostName = ""
    private var hostID = ""

    private var meetingId = ""
    private var meetingTitle = ""
    private var meetingAgenda = ""
    private var passCode = ""
    private var joinURL = ""
    private var meetingHostId = ""
    private var meetingStartTimestamp = ""
    private var isInMeeting = false

    private var roomName = ""
    private var meetingConfigDetails: MeetingHomeDetailsModel? = null
    private var meetingDetails: MeetingDetailsModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeetingAnalyticsBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        setupAdapter()
        hostName = sharedPref.getString(AppConstants.USER_NAME, null).toString()
        hostID = sharedPref.getString(AppConstants.USER_ID, null).toString()

        try {
            meetingConfigDetails = MeetingUserDetails.getMeetingConfigDetails()
            roomName = meetingConfigDetails!!.roomName!!

            meetingDetails = MeetingUserDetails.getMeetingDetails()
            meetingTitle = meetingDetails!!.topicName!!
            meetingAgenda = meetingDetails!!.agenda!!
            meetingId = meetingDetails!!.meetingNumber!!
            passCode = meetingDetails!!.meetingPasscode!!
            joinURL = meetingDetails!!.joinURL!!
            meetingHostId = meetingDetails!!.hostNumber!!
            meetingStartTimestamp = meetingDetails!!.meetingStartTimestamp!!
            isInMeeting = meetingDetails!!.inMeeting

        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.meetingName.text = meetingTitle
    }

    override fun onStart() {
        super.onStart()
        showProgDialog()
        fetchMeetingAnalytics { historyList ->
            dismissProgDialog()
            meetingsList.clear()
            meetingsList.addAll(historyList)
            meetingsList.sortByDescending { it.startTime }
            if(meetingsList.isEmpty()){
                binding.meetingsList.visibility = View.GONE
                binding.noDataAvailable.visibility = View.VISIBLE
            } else {
                binding.meetingsList.visibility = View.VISIBLE
                binding.noDataAvailable.visibility = View.GONE
            }
            recyclerViewAdapter!!.updateList(meetingsList)
        }
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.meetingsList.layoutManager = layoutManager
        recyclerViewAdapter = MeetingsAnalyticsAdapter(meetingsList, this)
        binding.meetingsList.adapter = recyclerViewAdapter
    }

    fun fetchMeetingAnalytics(onResult: (ArrayList<MeetingAnalyticsModel>) -> Unit) {

        val analyticsRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
            .document(roomName)
            .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
            .document(meetingId)
            .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)

        analyticsRef.get()
            .addOnSuccessListener { analyticsSnapshot ->

                if (analyticsSnapshot.isEmpty) {
                    onResult(arrayListOf())
                    return@addOnSuccessListener
                }

                val analyticsList = arrayListOf<MeetingAnalyticsModel>()

                var analyticsProcessed = 0
                val analyticsTotal = analyticsSnapshot.size()

                analyticsSnapshot.documents.forEach { analyticsDoc ->

                    val analytics = analyticsDoc.toObject(MeetingAnalyticsModel::class.java)
                            ?: MeetingAnalyticsModel()

                    analyticsRef.document(analyticsDoc.id)
                        .collection("participants")
                        .get()
                        .addOnSuccessListener { participantSnapshot ->

                            if (participantSnapshot.isEmpty) {
                                analyticsList.add(analytics)
                                analyticsProcessed++

                                if (analyticsProcessed == analyticsTotal) {
                                    analyticsList.sortByDescending {
                                        it.startTime?.toLongOrNull() ?: 0L
                                    }
                                    onResult(analyticsList)
                                }

                                return@addOnSuccessListener
                            }

                            val participants = arrayListOf<ParticipantModel>()

                            var participantsProcessed = 0
                            val participantsTotal = participantSnapshot.size()

                            participantSnapshot.documents.forEach { participantDoc ->

                                val participant = ParticipantModel(
                                    userId = participantDoc.getString("userId"),
                                    userName = participantDoc.getString("userName")
                                )

                                participantDoc.reference
                                    .collection("sessions")
                                    .get()
                                    .addOnSuccessListener { sessionSnapshot ->
                                        val sessions = arrayListOf<SessionModel>()
                                        sessionSnapshot.documents.forEach { sessionDoc ->
                                            sessions.add(
                                                SessionModel(
                                                    joinTime = sessionDoc.getString("joinTime"),
                                                    leaveTime = sessionDoc.getString("leaveTime")
                                                )
                                            )
                                        }
                                        //sessions.sortByDescending { it.joinTime?.toLongOrNull() ?: 0L }
                                        sessions.sortBy { it.joinTime?.toLongOrNull() ?: 0L }
                                        participant.sessions = sessions
                                        participants.add(participant)
                                        participantsProcessed++
                                        if (participantsProcessed == participantsTotal) {
                                            analytics.participants = participants
                                            analyticsList.add(analytics)
                                            analyticsProcessed++

                                            if (analyticsProcessed == analyticsTotal) {
                                                analyticsList.sortByDescending {
                                                    it.startTime?.toLongOrNull() ?: 0L
                                                }
                                                onResult(analyticsList)
                                            }
                                        }
                                    }
                                    .addOnFailureListener { e ->

                                        Log.e(TAG, "Session fetch error", e)
                                        participants.add(participant)
                                        participantsProcessed++

                                        if (participantsProcessed == participantsTotal) {
                                            analytics.participants = participants
                                            analyticsList.add(analytics)
                                            analyticsProcessed++
                                            if (analyticsProcessed == analyticsTotal) {
                                                analyticsList.sortByDescending {
                                                    it.startTime?.toLongOrNull() ?: 0L
                                                }
                                                onResult(analyticsList)
                                            }
                                        }
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Participant fetch error", e)
                            analyticsList.add(analytics)
                            analyticsProcessed++

                            if (analyticsProcessed == analyticsTotal) {
                                analyticsList.sortByDescending {
                                    it.startTime?.toLongOrNull() ?: 0L
                                }
                                onResult(analyticsList)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Analytics fetch error", e)
                onResult(arrayListOf())
            }
    }

    /*fun fetchMeetingAnalytics(onResult: (ArrayList<MeetingAnalyticsModel>) -> Unit) {
        try {
            val analyticsRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                .document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                .document(meetingId)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)

            analyticsRef.get()
                .addOnSuccessListener { snapshot ->
                    val analyticsList = arrayListOf<MeetingAnalyticsModel>()
                    if (snapshot.isEmpty) {
                        onResult(analyticsList)
                        return@addOnSuccessListener
                    }
                    val totalDocs = snapshot.documents.size
                    var completedDocs = 0

                    for (doc in snapshot.documents) {
                        val analytics = doc.toObject(MeetingAnalyticsModel::class.java)
                        if (analytics == null) {
                            completedDocs++
                            if (completedDocs == totalDocs) {
                                onResult(analyticsList)
                            }
                            continue
                        }
                        // Fetch participants subcollection
                        analyticsRef.document(doc.id)
                            .collection("participants")
                            .get()
                            .addOnSuccessListener { participantSnapshot ->
                                val participants = arrayListOf<ParticipantModel>()
                                for (participantDoc in participantSnapshot.documents) {
                                    val participant = participantDoc.toObject(ParticipantModel::class.java)
                                    if (participant != null) {
                                        participants.add(participant)
                                    }
                                }
                                analytics.participants = participants
                                analyticsList.add(analytics)
                                completedDocs++
                                if (completedDocs == totalDocs) {
                                    analyticsList.sortByDescending {
                                        it.startTime?.toLongOrNull() ?: 0L
                                    }
                                    onResult(analyticsList)
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Participant fetch error: ${e.message}")
                                analyticsList.add(analytics)
                                completedDocs++
                                if (completedDocs == totalDocs) {
                                    analyticsList.sortByDescending {
                                        it.startTime?.toLongOrNull() ?: 0L
                                    }
                                    onResult(analyticsList)
                                }
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Analytics fetch error: ${e.message}")
                    onResult(arrayListOf())
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }*/


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun oSelectMeetingHistory(meetingDetails: MeetingAnalyticsModel?) {
        MeetingUserDetails.setMeetingAnalyticsDetails(meetingDetails!!)
        val next = Intent(this@MeetingsAnalyticsActivity, MeetingsAnalyticsDetailsActivity::class.java)
        startActivity(next)
    }

}