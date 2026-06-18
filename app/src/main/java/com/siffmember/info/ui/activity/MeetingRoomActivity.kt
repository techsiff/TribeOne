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
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.MeetingUserDetails
import com.siffmember.info.utils.Utils

class MeetingRoomActivity : BaseActivity(), MeetingsAdapter.MeetingsListener {

    companion object {
        var TAG = "MeetingRoomActivity"
    }
    private lateinit var binding: ActivityMeetingRoomBinding
    private lateinit var db: FirebaseFirestore
    private var meetingsList: ArrayList<MeetingDetailsModel> = ArrayList()
    private var recyclerViewAdapter: MeetingsAdapter? = null
    private var hostName = ""
    private var hostID = ""
    private var roomName = ""
    private var accountUserEmail = ""
    private var accountId = ""
    private var appClientId = ""
    private var appClientSecret = ""
    private var serverClientId = ""
    private var serverClientSecret = ""
    private var meetingConfigDetails: MeetingHomeDetailsModel? = null

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
        try {
            meetingConfigDetails = MeetingUserDetails.getMeetingConfigDetails()
            roomName = meetingConfigDetails!!.roomName!!
            accountUserEmail = meetingConfigDetails!!.accountUserEmail!!
            accountId = meetingConfigDetails!!.accountId!!
            appClientId = meetingConfigDetails!!.appClientId!!
            appClientSecret = meetingConfigDetails!!.appClientSecret!!
            serverClientId = meetingConfigDetails!!.serverClientId!!
            serverClientSecret = meetingConfigDetails!!.serverClientSecret!!
        } catch (e: Exception) {
            e.printStackTrace()
        }
        binding.meetingRoomsTitle.text = roomName
        binding.btnNewMeeting.visibility =
            if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.VISIBLE else View.GONE
        binding.btnNewMeeting.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingRoomActivity)) {
                val next = Intent(this@MeetingRoomActivity, MeetingCreateActivity::class.java)
                startActivity(next)
            } else {
                Toast.makeText(this@MeetingRoomActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }

    }

    override fun onStart() {
        super.onStart()
        try {
            showProgDialog()
            fetchAllMeetings(hostID) { historyList ->
                meetingsList.clear()
                meetingsList.addAll(historyList)
                meetingsList.sortByDescending { it.timestamp }
                recyclerViewAdapter!!.updateList(meetingsList)
            }
        }catch (e: Exception){
            e.printStackTrace()
            dismissProgDialog()
        }
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.meetingsList.layoutManager = layoutManager
        recyclerViewAdapter = MeetingsAdapter(meetingsList, this)
        binding.meetingsList.adapter = recyclerViewAdapter
    }

    fun fetchAllMeetings(userId: String, onResult: (List<MeetingDetailsModel>) -> Unit) {
        db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS).document(roomName).collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
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
        if(Utils.isNetworkAvailable(this@MeetingRoomActivity)) {
            MeetingUserDetails.clear()
            MeetingUserDetails.setUsers(meetingDetails!!.members)
            MeetingUserDetails.setMeetingDetails(meetingDetails)
            val next = Intent(this@MeetingRoomActivity, MeetingJoiningActivity::class.java)
            startActivity(next)
        } else {
            Toast.makeText(this@MeetingRoomActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
        }

    }

}