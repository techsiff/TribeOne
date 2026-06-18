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
import com.siffmember.info.databinding.ActivityMeetingHomeBinding
import com.siffmember.info.ui.adapter.MeetingsHomeAdapter
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.MeetingUserDetails
import com.siffmember.info.utils.Utils

class MeetingsHomeActivity : BaseActivity(), MeetingsHomeAdapter.MeetingsListener {

    companion object {
        var TAG = "MeetingsHomeActivity"
    }
    private lateinit var binding: ActivityMeetingHomeBinding
    private lateinit var db: FirebaseFirestore
    private var meetingsList: ArrayList<MeetingHomeDetailsModel> = ArrayList()
    private var recyclerViewAdapter: MeetingsHomeAdapter? = null
    private var hostName = ""
    private var hostID = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeetingHomeBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnNewMeetingRoomLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        setupAdapter()
        MeetingUserDetails.clear()
        hostName = sharedPref.getString(AppConstants.USER_NAME, null).toString()
        hostID = sharedPref.getString(AppConstants.USER_ID, null).toString()

        val allowedHostIds = setOf("9845143724", "4431548788", "9994893355")
        binding.btnNewMeetingRoom.visibility = if (hostID in allowedHostIds) View.VISIBLE else View.INVISIBLE

        binding.btnNewMeetingRoom.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingsHomeActivity)) {
                val next = Intent(this@MeetingsHomeActivity, MeetingCreateRoomActivity::class.java)
                startActivity(next)
            } else {
                Toast.makeText(this@MeetingsHomeActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }
        binding.btnBlocking.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.VISIBLE else View.GONE
        binding.btnTags.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false) || sharedPref.getBoolean(AppConstants.IS_EDIT_ACCESS, false)) View.VISIBLE else View.GONE
        binding.btnBlocking.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingsHomeActivity)) {
                val next = Intent(this@MeetingsHomeActivity, UserBlockActivity::class.java)
                startActivity(next)
            } else {
                Toast.makeText(this@MeetingsHomeActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }
        binding.btnTags.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingsHomeActivity)) {
                val next = Intent(this@MeetingsHomeActivity, UserTagsActivity::class.java)
                startActivity(next)
            } else {
                Toast.makeText(this@MeetingsHomeActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        showProgDialog()
        fetchAllMeetings { historyList ->
            meetingsList.clear()
            meetingsList.addAll(historyList)
            //meetingsList.sortByDescending { it.roomName }
            recyclerViewAdapter!!.updateList(meetingsList)
        }
    }

    private fun setupAdapter(){
        val layoutManager = LinearLayoutManager(this)
        binding.meetingsList.layoutManager = layoutManager
        recyclerViewAdapter = MeetingsHomeAdapter(meetingsList, this)
        binding.meetingsList.adapter = recyclerViewAdapter
    }

    fun fetchAllMeetings(onResult: (List<MeetingHomeDetailsModel>) -> Unit) {
        db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
            .get()
            .addOnSuccessListener { snapshot ->
                Log.e(TAG, "All meetings: ${snapshot.documents.size}")
                val result = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(MeetingHomeDetailsModel::class.java)
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

    override fun oSelectMeetingRoom(meetingDetails: MeetingHomeDetailsModel?) {
        MeetingUserDetails.setMeetingConfigDetails(meetingDetails!!)
        val next = Intent(this@MeetingsHomeActivity, MeetingRoomActivity::class.java)
        startActivity(next)
    }

}