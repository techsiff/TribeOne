package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.NotificationRequest
import com.siffmember.info.data.remote.model.functions.NotificationResponse
import com.siffmember.info.data.remote.model.zoom.CreateMeetingPayload
import com.siffmember.info.data.remote.model.zoom.ZoomMeetingDeleteRequest
import com.siffmember.info.data.remote.model.zoom.ZoomMeetingRequest
import com.siffmember.info.data.remote.model.zoom.meeting.ZoomMeetingResponse
import com.siffmember.info.databinding.ActivityMeetingCreateBinding
import com.siffmember.info.ui.fragment.AddMeetingMemberBottomSheetFragment
import com.siffmember.info.ui.fragment.NewMeetingCreateUsersBottomSheetFragment
import com.siffmember.info.ui.fragment.SelectGroupBottomSheetFragment
import com.siffmember.info.ui.fragment.SelectTaggedUsersBottomSheetFragment
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.MembersZoomMeeting
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.ExcelUtil
import com.siffmember.info.utils.MeetingCreateUserDetails
import com.siffmember.info.utils.MeetingUserDetails
import com.siffmember.info.utils.Utils
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class MeetingCreateActivity : BaseActivity(), AddMeetingMemberBottomSheetFragment.BottomSheetListener, SelectGroupBottomSheetFragment.BottomSheetListener, NewMeetingCreateUsersBottomSheetFragment.BottomSheetListener,
    SelectTaggedUsersBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "MeetingCreateActivity"
    }

    private lateinit var binding: ActivityMeetingCreateBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var viewModel: CommunityViewModel
    private lateinit var postsViewModel: PostsMessageViewModel
    private var meetingTitle = ""
    private var meetingAgenda = ""
    private var meetingDate = ""
    private var meetingTime = ""
    private var adminName = ""
    private var adminID = ""
    private var meetingId = ""
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>
    private var selectedDateTime: ZonedDateTime = ZonedDateTime.now(ZoneId.systemDefault())
    private var createdDateTimeMillis: Long = 0L
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
        binding = ActivityMeetingCreateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnCreateMeetingLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        db = Firebase.firestore
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        try {
            adminName = sharedPref.getString(AppConstants.USER_NAME, null)!!
            adminID = sharedPref.getString(AppConstants.USER_ID, null)!!
            meetingConfigDetails = MeetingUserDetails.getMeetingConfigDetails()
            roomName = meetingConfigDetails!!.roomName!!
            accountUserEmail = meetingConfigDetails!!.accountUserEmail!!
            accountId = meetingConfigDetails!!.accountId!!
            appClientId = meetingConfigDetails!!.appClientId!!
            appClientSecret = meetingConfigDetails!!.appClientSecret!!
            serverClientId = meetingConfigDetails!!.serverClientId!!
            serverClientSecret = meetingConfigDetails!!.serverClientSecret!!
            /*val vBundle = intent.extras
            if (vBundle != null) {
                roomName = vBundle.getString("roomName", null)
                accountUserEmail = vBundle.getString("accountUserEmail", null)
                accountId = vBundle.getString("accountId", null)
                appClientId = vBundle.getString("appClientId", null)
                appClientSecret = vBundle.getString("appClientSecret", null)
                serverClientId = vBundle.getString("serverClientId", null)
                serverClientSecret = vBundle.getString("serverClientSecret", null)
            }*/
        }catch (e: Exception){
            e.printStackTrace()
        }

        binding.btnAddMembers.setOnClickListener {
            if (validate()) {
                val bottomSheetFragment = AddMeetingMemberBottomSheetFragment()
                val bundle = Bundle()
                bundle.putString("adminId", adminID)
                bundle.putString("addType", "1")
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }

        binding.btnSelectTags.setOnClickListener {
            if (validate()) {
                val bottomSheetFragment = SelectTaggedUsersBottomSheetFragment()
                val bundle = Bundle()
                bundle.putString("adminId", adminID)
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }

        binding.btnSelectCategory.setOnClickListener {
            if (validate()) {
                val bottomSheetFragment = AddMeetingMemberBottomSheetFragment()
                val bundle = Bundle()
                bundle.putString("adminId", adminID)
                bundle.putString("addType", "2")
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }
        binding.btnUploadMembers.setOnClickListener {
            if (validate()) {
                openFilePicker()
            }
        }
        binding.btnSelectGroups.setOnClickListener {
            if (validate()) {
                val bottomSheetFragment = SelectGroupBottomSheetFragment()
                val bundle = Bundle()
                bundle.putString("adminId", adminID)
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }
        binding.etDate.setOnClickListener {
            if (validate()) {
                openDatePicker()
            }
        }
        binding.etTime.setOnClickListener {
            if (validate()) {
                openTimePicker()
            }
        }
        binding.btnTotalUsers.setOnClickListener {
            if(MeetingCreateUserDetails.getUsers().isNotEmpty()){
                val bottomSheetFragment = NewMeetingCreateUsersBottomSheetFragment()
                val bundle = Bundle()
                bundle.putString("adminId", adminID)
                bottomSheetFragment.arguments = bundle
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }

        binding.btnCreateMeeting.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingCreateActivity)) {
                if (validate()) {
                    if(MeetingCreateUserDetails.getUsers().isEmpty()){
                        Toast.makeText(this@MeetingCreateActivity,"Please select users",Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    createMeeting(MeetingCreateUserDetails.getUsers())
                }
            } else {
                Toast.makeText(this@MeetingCreateActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }

        }

        // Initial load – show current date
        binding.etDate.setText(
            selectedDateTime.format(
                DateTimeFormatter.ofPattern("dd MMM yyyy")
            )
        )

        // Initial load – show current time
        binding.etTime.setText(
            selectedDateTime.format(
                DateTimeFormatter.ofPattern("hh:mm a")
            )
        )

        val finalMillis = selectedDateTime
            .toInstant()
            .toEpochMilli()
        createdDateTimeMillis = finalMillis
        Log.e("DATE_TIME", "Final millis = $createdDateTimeMillis")

        // Initialize the ActivityResultLauncher
        fileSelectorLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK) {
                val data: Intent? = result.data
                val uri: Uri? = data?.data
                if (uri != null) {
                    //Log.e("AddUserActivity","${uri.path}")
                    showProgDialog()
                    importExcelUsers(uri)
                }
            }
        }
    }

    private fun openDatePicker() {

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select Date")
            .setSelection(selectedDateTime.toInstant().toEpochMilli())
            .build()

        datePicker.show(supportFragmentManager, "DATE_PICKER")

        datePicker.addOnPositiveButtonClickListener { millis ->
            selectedDateTime = Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .withHour(selectedDateTime.hour)
                .withMinute(selectedDateTime.minute)

            binding.etDate.setText(
                selectedDateTime.format(
                    DateTimeFormatter.ofPattern("dd MMM yyyy")
                )
            )
            openTimePicker()
        }
    }

    private fun openTimePicker() {

        val timePicker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H) // or CLOCK_24H
            .setHour(selectedDateTime.hour)
            .setMinute(selectedDateTime.minute)
            .setTitleText("Select Time")
            .build()

        timePicker.show(supportFragmentManager, "TIME_PICKER")

        timePicker.addOnPositiveButtonClickListener {
            selectedDateTime = selectedDateTime
                .withHour(timePicker.hour)
                .withMinute(timePicker.minute)

            binding.etTime.setText(
                selectedDateTime.format(
                    DateTimeFormatter.ofPattern("hh:mm a")
                )
            )
            val finalMillis = selectedDateTime
                .toInstant()
                .toEpochMilli()
            createdDateTimeMillis = finalMillis
            Log.e("DATE_TIME", "Final millis = $createdDateTimeMillis")
        }
    }

    // Function to launch the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/*"
        }
        fileSelectorLauncher.launch(intent)
    }


    private fun validate(): Boolean{
        if(binding.meetingTitleEdit.text.toString().isNotEmpty()){
            meetingTitle = binding.meetingTitleEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateActivity,"Please enter meeting title", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.meetingAgendaEdit.text.toString().isNotEmpty()){
            meetingAgenda = binding.meetingAgendaEdit.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateActivity,"Please enter meeting agenda", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.etDate.text.toString().isNotEmpty()){
            meetingDate = binding.etDate.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateActivity,"Please select date", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.etTime.text.toString().isNotEmpty()){
            meetingTime = binding.etTime.text.toString()
        } else {
            Toast.makeText(this@MeetingCreateActivity,"Please select time", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
             MeetingCreateUserDetails.clear()
             finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        MeetingCreateUserDetails.clear()
    }

    @SuppressLint("SetTextI18n")
    override fun onAddMember(selectedUsersList: List<MembersGroup>) {
        val existingUsers = MeetingCreateUserDetails.getUsers()
        val newUsers = selectedUsersList.filter { newUser ->
            existingUsers.none { it.id == newUser.id }
        }
        newUsers.forEach {
            MeetingCreateUserDetails.addUser(it)
        }
        binding.totalUsersTxt.text =
            "Total Users: ${MeetingCreateUserDetails.getUsers().size}"
    }


    private fun createMeeting(selectedUsersList: List<MembersGroup>){
        try{
            showProgDialog()
            MeetingUserDetails.clear()
            val membersIds = mutableListOf<String>()
            val members = mutableListOf<MembersZoomMeeting>()
            for (user in selectedUsersList) {
                membersIds.add(user.phoneNumber)
                members.add(MembersZoomMeeting(user.phoneNumber, user.name, user.fcmToken))
            }
            membersIds.add(adminID)
            members.add(MembersZoomMeeting(adminID, adminName))
            //val createdAtMillis = System.currentTimeMillis()
            createNewMeeting(meetingTitle, meetingAgenda, createdDateTimeMillis) { success, meetingId, joinURL, passCode ->
                if (success) {
                    addMeetings(MeetingDetailsModel(
                        meetingTitle,
                        meetingAgenda,
                        adminName,
                        adminID,
                        meetingId,
                        passCode,
                        joinURL,
                        createdDateTimeMillis.toString(),
                        membersIds,
                        members,
                        "",
                        false)){ isAdded ->
                        if(isAdded){
                            val newMemberTokens = selectedUsersList
                                .filter { it.id != adminID && it.fcmToken.isNotEmpty() }
                                .map { it.fcmToken }
                                .distinct()
                            val body = "$adminName invited you to join meeting"
                            sendNotification(meetingTitle, body, newMemberTokens){
                                //if(isSent){
                                runOnUiThread {
                                    Log.e(TAG,"getMeetingSignature ::$appClientId $appClientSecret")
                                    MeetingUserDetails.clear()
                                    MeetingUserDetails.setUsers(members)
                                    MeetingUserDetails.setMeetingDetails(
                                        MeetingDetailsModel(
                                            topicName = meetingTitle,
                                            agenda = meetingAgenda,
                                            meetingNumber = meetingId,
                                            meetingPasscode = passCode,
                                            joinURL = joinURL,
                                            timestamp = createdDateTimeMillis.toString(),
                                            hostName = adminName,
                                            hostNumber = adminID,
                                            memberIds = membersIds,
                                            inMeeting = false
                                        ))
                                    MeetingCreateUserDetails.clear()
                                    val next = Intent(this@MeetingCreateActivity, MeetingJoiningActivity::class.java)
                                    startActivity(next)
                                    finish()
                                }
                            }
                        }
                    }
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun deleteZoomMeeting(meetingId: String) {
        try {
            val request = ZoomMeetingDeleteRequest(
                meetingId = meetingId,
                occurrenceId = "0",
                scheduleForReminder = true,
                cancelMeetingReminder = true,
                serverClientId,
                serverClientSecret,
                accountId
            )
            RetrofitInstanceFunction.api.deleteMeeting(request)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                        if (response.isSuccessful) {
                            val response = response.body()
                            if (response?.string() != null) {
                                // Notification sent successfully
                                Log.e(TAG, "delete Meeting: ${response.string()}")
                                //onComplete(true)
                            }
                        } else {
                            // Handle error response
                            Log.e(TAG, "Error delete Meeting: ${response.errorBody()}")
                            //onComplete(false)
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        // Handle failure
                        Log.e(TAG, "Failed to delete Meeting: ${t.message}")
                        //onComplete(false)
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createNewMeeting(title: String, agenda: String, createdAtMillis: Long, onComplete: (Boolean, String, String, String) -> Unit) {
        try{
            val formattedTime = Instant.ofEpochMilli(createdAtMillis)
                .atZone(ZoneOffset.UTC)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
            Log.e(TAG,"formattedTime: $formattedTime")
            val request = ZoomMeetingRequest(
                CreateMeetingPayload(topic = title, agenda = agenda, start_time = formattedTime),
                serverClientId,
                serverClientSecret,
                accountId
            )

            RetrofitInstanceFunction.api.createMeeting(request).enqueue(object : Callback<ZoomMeetingResponse> {
                override fun onResponse(call: Call<ZoomMeetingResponse>, response: Response<ZoomMeetingResponse>) {
                    if (response.isSuccessful) {
                        val meetingResponse = response.body()
                        if (meetingResponse?.id != null) {
                            // Notification sent successfully
                            Log.e(TAG,"createNewMeeting: ${meetingResponse.id}")
                            meetingId = meetingResponse.id.toString()
                            val joinURL = meetingResponse.join_url
                            val passCode = meetingResponse.password
                            if(passCode.length == 6){
                                onComplete(true, meetingId, joinURL, passCode)
                            } else {
                                Toast.makeText(this@MeetingCreateActivity,"Failed to create new meeting try again",Toast.LENGTH_SHORT).show()
                                deleteZoomMeeting(meetingId)
                                onComplete(false,"","","")
                            }
                        }
                    } else {
                        // Handle error response
                        Log.e(TAG,"Error createNewMeeting: ${response.errorBody()}")
                        Toast.makeText(this@MeetingCreateActivity,"Failed to create new meeting",Toast.LENGTH_SHORT).show()
                        onComplete(false,"","","")
                        dismissProgDialog()
                    }
                }

                override fun onFailure(call: Call<ZoomMeetingResponse>, t: Throwable) {
                    // Handle failure
                    Log.e(TAG,"Failed to createNewMeeting: ${t.message}")
                    Toast.makeText(this@MeetingCreateActivity,"Failed to create new meeting",Toast.LENGTH_SHORT).show()
                    onComplete(false,"","","")
                    dismissProgDialog()
                }
            })
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun addMeetings(meetings: MeetingDetailsModel, onComplete: (Boolean) -> Unit){
        try{
            //db.collection(AppConstants.TABLE_MEETING_DETAILS).document(meetings.meetingNumber!!)
            db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS).document(roomName).collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS).document(meetings.meetingNumber!!)
                .set(meetings)
                .addOnSuccessListener {
                    Log.e(TAG, "DocumentSnapshot successfully written!")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@MeetingCreateActivity,"Failed to createNewMeeting",Toast.LENGTH_SHORT).show()
                    dismissProgDialog()
                    onComplete(false)
                }
        }catch (e: Exception){
            e.printStackTrace()
            onComplete(false)
        }
    }

    private fun sendNotification(title: String, body: String, tokens: List<String>, onComplete: (Boolean) -> Unit) {
        val senderName = sharedPref.getString(AppConstants.USER_NAME, null)
        val senderID = sharedPref.getString(AppConstants.USER_ID, null)

        val notificationRequest = NotificationRequest(
            tokens = tokens,  // Replace with the actual FCM token
            title = title,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to "NewMeeting",
                "groupId" to "",
                "groupName" to "",
                "senderId" to senderID.toString(),
                "senderName" to senderName.toString(),
                "postTitle" to "",
                "postId" to "",
                "postContent" to "",
                "timestamp" to "",
            )
        )
        RetrofitInstanceFunction.api.sendNotification(notificationRequest).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (response.isSuccessful) {
                    val notificationResponse = response.body()
                    if (notificationResponse?.success != null) {
                        // Notification sent successfully
                        Log.e(TAG,"Notification sent: ${notificationResponse.success}")
                        onComplete(true)
                        dismissProgDialog()
                    }
                } else {
                    // Handle error response
                    Log.e(TAG,"Error sending notification: ${response.errorBody()}")
                    onComplete(false)
                    dismissProgDialog()
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                // Handle failure
                Log.e(TAG,"Failed to send notification: ${t.message}")
                onComplete(false)
                dismissProgDialog()
            }
        })
    }
    
    private var usersFromFileList: ArrayList<MembersGroup> = ArrayList()

    @OptIn(DelicateCoroutinesApi::class)
    private fun importExcelUsers(uri: Uri) {
        try {
            usersFromFileList.clear()
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "doInBackground: Importing...")
                    runOnUiThread {
                        Toast.makeText(this@MeetingCreateActivity, "Importing...", Toast.LENGTH_SHORT).show()
                    }
                    val readExcelNew: List<Map<Int, Any>> = ExcelUtil.readExcelNew(this@MeetingCreateActivity, uri, uri.path)
                    Log.i(TAG, "onActivityResult:readExcelNew: ${ readExcelNew.size} ")
                    if (readExcelNew.isNotEmpty()) {
                        Log.i(TAG, "run: successfully imported")
                        runOnUiThread {
                            Toast.makeText(this@MeetingCreateActivity, "successfully imported", Toast.LENGTH_SHORT).show()
                        }

                        for (i in readExcelNew.indices) {
                            val map = readExcelNew[i]
                            val name = map[0]?.toString() ?: ""
                            //val email = map[1]?.toString() ?: ""
                            //val country = map[2]?.toString() ?: ""
                            val phoneNumber = map[3]?.toString() ?: ""
                            //val category = map[4]?.toString() ?: ""

                            if (name.isNotEmpty() && name != "Name" && phoneNumber.isNotEmpty()) {
                                val user = MembersGroup(phoneNumber, name, phoneNumber, false, "")
                                usersFromFileList.add(user)
                            }
                        }
                        if(usersFromFileList.isNotEmpty()){
                            Log.e(TAG, " Users list:: ${usersFromFileList.size}")
                            onAddMember(usersFromFileList)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MeetingCreateActivity, "No data available", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            dismissProgDialog()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onUpdateMember() {
        binding.totalUsersTxt.text = "Total Users: ${MeetingCreateUserDetails.getUsers().size}"
    }
}