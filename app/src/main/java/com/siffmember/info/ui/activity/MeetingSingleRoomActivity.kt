package com.siffmember.info.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityMeetingRoomBinding
import com.siffmember.info.ui.adapter.MeetingsAdapter
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.MeetingUserDetails
import com.siffmember.info.utils.Utils

class MeetingSingleRoomActivity : BaseActivity(), MeetingsAdapter.MeetingsListener {

    companion object {
        var TAG = "MeetingSingleRoomActivity"
    }
    private lateinit var binding: ActivityMeetingRoomBinding
    private lateinit var db: FirebaseFirestore
    private var meetingsList: ArrayList<MeetingDetailsModel> = ArrayList()
    private var recyclerViewAdapter: MeetingsAdapter? = null
    private var hostName = ""
    private var hostID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeetingRoomBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnNewMeetingLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        setupAdapter()
        MeetingUserDetails.clear()
        hostName = sharedPref.getString(AppConstants.USER_NAME, null).toString()
        hostID = sharedPref.getString(AppConstants.USER_ID, null).toString()

        binding.btnNewMeeting.visibility =
            if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.VISIBLE else View.GONE
        binding.btnNewMeeting.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingSingleRoomActivity)) {
                val next = Intent(this@MeetingSingleRoomActivity, MeetingCreateActivity::class.java)
                startActivity(next)
            } else {
                Toast.makeText(this@MeetingSingleRoomActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }
        /*binding.btnBlocking.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.VISIBLE else View.GONE
        binding.btnTags.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.VISIBLE else View.GONE
        binding.btnBlocking.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingSingleRoomActivity)) {
                val next = Intent(this@MeetingSingleRoomActivity, UserBlockActivity::class.java)
                startActivity(next)
            } else {
                Toast.makeText(this@MeetingSingleRoomActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }
        binding.btnTags.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingSingleRoomActivity)) {
                val next = Intent(this@MeetingSingleRoomActivity, UserTagsActivity::class.java)
                startActivity(next)
            } else {
                Toast.makeText(this@MeetingSingleRoomActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }*/
    }

    override fun onStart() {
        super.onStart()
        showProgDialog()
        fetchAllMeetings(hostID) { historyList ->
            meetingsList.clear()
            meetingsList.addAll(historyList)
            meetingsList.sortByDescending { it.timestamp }
            recyclerViewAdapter!!.updateList(meetingsList)
        }
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.meetingsList.layoutManager = layoutManager
        recyclerViewAdapter = MeetingsAdapter(meetingsList, this)
        binding.meetingsList.adapter = recyclerViewAdapter
    }

    fun fetchAllMeetings(userId: String, onResult: (List<MeetingDetailsModel>) -> Unit) {
        db.collection(AppConstants.TABLE_MEETING_DETAILS)
            //.orderBy("timestamp", Query.Direction.DESCENDING)
            .whereArrayContains("memberIds", userId)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.e(TAG, "All meetings: ${snapshot.documents.size}")
                val result = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MeetingDetailsModel::class.java)
                }
                onResult(result)
                dismissProgDialog()
            }
            .addOnFailureListener {
                Log.e(TAG, "Error fetching meetings: ${it.message}")
                onResult(emptyList())
                dismissProgDialog()
            }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onJoinMeeting(meetingDetails: MeetingDetailsModel?) {
        MeetingUserDetails.clear()
        MeetingUserDetails.setUsers(meetingDetails!!.members)
        val next = Intent(this@MeetingSingleRoomActivity, MeetingJoiningActivity::class.java)
        next.putExtra("meetingTitle", meetingDetails.topicName)
        next.putExtra("meetingAgenda", meetingDetails.agenda)
        next.putExtra("meetingId", meetingDetails.meetingNumber)
        next.putExtra("passCode", meetingDetails.meetingPasscode)
        next.putExtra("joinURL", meetingDetails.joinURL)
        next.putExtra("meetingHostId", meetingDetails.hostNumber)
        next.putExtra("meetingTimestamp", meetingDetails.timestamp)
        startActivity(next)
    }

}