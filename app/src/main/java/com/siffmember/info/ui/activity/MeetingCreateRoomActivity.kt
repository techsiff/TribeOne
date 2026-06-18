package com.siffmember.info.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityMeetingCreateRoomBinding
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.Utils

class MeetingCreateRoomActivity : BaseActivity() {

    companion object {
        var TAG = "MeetingCreateRoomActivity"
    }
    private lateinit var binding: ActivityMeetingCreateRoomBinding
    private lateinit var db: FirebaseFirestore
    private var roomName = ""
    private var accountUserEmail = ""
    private var accountId = ""
    private var appClientId = ""
    private var appClientSecret = ""
    private var serverClientId = ""
    private var serverClientSecret = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMeetingCreateRoomBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        db = Firebase.firestore
        val adapter = ArrayAdapter(this, R.layout.spinner_list, Utils.category)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)

        binding.apply {
            btnAddRoom.setOnClickListener {
                if (validate()) {
                    showProgDialog()
                    val roomDetails = MeetingHomeDetailsModel(roomName, accountUserEmail, accountId, appClientId, appClientSecret, serverClientId, serverClientSecret)
                    addRoom(roomDetails)
                }
            }
        }

    }

    private fun validate(): Boolean{
        if(binding.roomNameEdit.text.toString().trim().isNotEmpty()){
            roomName = binding.roomNameEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateRoomActivity,"Please enter room name", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.roomAccountIdEdit.text.toString().trim().isNotEmpty()){
            accountId = binding.roomAccountIdEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateRoomActivity,"Please enter account id", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.roomAccountEmailEdit.text.toString().trim().isNotEmpty()){
            accountUserEmail = binding.roomAccountEmailEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateRoomActivity,"Please enter account email id", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.roomAppClientIdEdit.text.toString().trim().isNotEmpty()){
            appClientId = binding.roomAppClientIdEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateRoomActivity,"Please enter account app client id", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.roomAppClientSecretEdit.text.toString().trim().isNotEmpty()){
            appClientSecret = binding.roomAppClientSecretEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateRoomActivity,"Please enter account app client secret", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.roomServerClientIdEdit.text.toString().trim().isNotEmpty()){
            serverClientId = binding.roomServerClientIdEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateRoomActivity,"Please enter account server client id", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.roomServerClientSecretEdit.text.toString().trim().isNotEmpty()){
            serverClientSecret = binding.roomServerClientSecretEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateRoomActivity,"Please enter account server client secret", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun addRoom(roomDetails: MeetingHomeDetailsModel){
        try{
            db.collection(AppConstants.TABLE_USER_DETAILS).document(roomDetails.roomName!!)
                .set(roomDetails)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    binding.roomNameEdit.setText("")
                    binding.roomAccountIdEdit.setText("")
                    binding.roomAccountEmailEdit.setText("")
                    binding.roomAppClientIdEdit.setText("")
                    binding.roomAppClientSecretEdit.setText("")
                    binding.roomServerClientIdEdit.setText("")
                    binding.roomServerClientSecretEdit.setText("")
                    roomName = ""
                    accountId = ""
                    accountUserEmail = ""
                    appClientId = ""
                    appClientSecret = ""
                    serverClientId = ""
                    serverClientSecret = ""
                    dismissProgDialog()
                    Toast.makeText(this@MeetingCreateRoomActivity,"Room added successfully", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error writing document", e)
                    Toast.makeText(this@MeetingCreateRoomActivity,"Room failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}