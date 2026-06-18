package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.application.GlobalMeetingListener
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.NotificationRequest
import com.siffmember.info.data.remote.model.functions.NotificationResponse
import com.siffmember.info.data.remote.model.zoom.SignatureRequest
import com.siffmember.info.data.remote.model.zoom.SignatureResponse
import com.siffmember.info.data.remote.model.zoom.ZakRequest
import com.siffmember.info.data.remote.model.zoom.ZakResponse
import com.siffmember.info.data.remote.model.zoom.ZoomMeetingDeleteRequest
import com.siffmember.info.databinding.ActivityMeetingJoiningBinding
import com.siffmember.info.ui.adapter.MeetingParticipantsAdapter
import com.siffmember.info.ui.fragment.AddMeetingMemberBottomSheetFragment
import com.siffmember.info.ui.fragment.SelectGroupBottomSheetFragment
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.ui.model.MembersGroup
import com.siffmember.info.ui.model.MembersZoomMeeting
import com.siffmember.info.ui.zoom.UserLoginCallback
import com.siffmember.info.ui.zoom.UserLoginCallback.ZoomDemoAuthenticationListener
import com.siffmember.info.ui.zoom.initsdk.InitAuthSDKCallback
import com.siffmember.info.ui.zoom.initsdk.InitAuthSDKHelper
import com.siffmember.info.ui.zoom.zoommeetingui.CustomNewZoomUIActivity
import com.siffmember.info.ui.zoom.zoommeetingui.ZoomMeetingUISettingHelper
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.ExcelUtil
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
import us.zoom.sdk.InMeetingNotificationHandle
import us.zoom.sdk.JoinMeetingParams
import us.zoom.sdk.MeetingParameter
import us.zoom.sdk.MeetingServiceListener
import us.zoom.sdk.MeetingStatus
import us.zoom.sdk.SimpleZoomUIDelegate
import us.zoom.sdk.StartMeetingOptions
import us.zoom.sdk.StartMeetingParamsWithoutLogin
import us.zoom.sdk.ZoomApiError
import us.zoom.sdk.ZoomAuthenticationError
import us.zoom.sdk.ZoomError
import us.zoom.sdk.ZoomSDK

class MeetingJoiningActivity : BaseActivity(), InitAuthSDKCallback, MeetingServiceListener,
    ZoomDemoAuthenticationListener, MeetingParticipantsAdapter.CommunityUserGroupListener,
    AddMeetingMemberBottomSheetFragment.BottomSheetListener, SelectGroupBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "MeetingJoiningActivity"
    }

    private lateinit var binding: ActivityMeetingJoiningBinding
    private var mZoomSDK: ZoomSDK? = null
    private var isResumed = false
    private var isMeetingMinimized = false
    private lateinit var db: FirebaseFirestore
    private var hostName = ""
    private var hostID = ""
    private var meetingId = ""
    private var occurrenceId = "0"
    private var meetingTitle = ""
    private var meetingAgenda = ""
    private var passCode = ""
    private var joinURL = ""
    private var meetingHostId = ""
    private var meetingStartTimestamp = ""
    private var isInMeeting = false
    private var userAdapter: MeetingParticipantsAdapter? = null
    private var userList: ArrayList<MembersZoomMeeting> = ArrayList()
    private lateinit var fileSelectorLauncher: ActivityResultLauncher<Intent>

    private var roomName = ""
    private var accountUserEmail = ""
    private var accountId = ""
    private var appClientId = ""
    private var appClientSecret = ""
    private var serverClientId = ""
    private var serverClientSecret = ""
    private var meetingConfigDetails: MeetingHomeDetailsModel? = null
    private var meetingDetails: MeetingDetailsModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mZoomSDK = ZoomSDK.getInstance()
        binding = ActivityMeetingJoiningBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnJoinMeetingLL) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemBars.bottom)
            insets
        }
        setupAdapter()
        //binding.btnJoiningMeeting.visibility = View.GONE
        isMeetingMinimized = false
        db = Firebase.firestore
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
        binding.meetingTitle.text = meetingTitle
        binding.meetingAgenda.text = meetingAgenda
        binding.btnDeleteMeeting.visibility = if (meetingHostId == hostID) View.VISIBLE else View.GONE
        //binding.btnMeetingAnalytics.visibility = if (meetingHostId == hostID) View.VISIBLE else View.GONE
        binding.btnMeetingAnalytics.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false)) View.VISIBLE else View.GONE
        binding.btnRefreshMeeting.visibility = if (meetingHostId == hostID) View.GONE else View.VISIBLE
        binding.btnJoiningMeeting.text = if (meetingHostId == hostID) "Start Meeting" else "Join Meeting"
        initZoomSDK()

        /*val meetingTime = Instant.ofEpochMilli(meetingTimestamp.toLong())
        val now = Instant.now()

        if (meetingTime.isAfter(now)) {
            // valid
        } else {
            // invalid
        }*/
        if(meetingHostId == hostID){
            binding.addUploadLL.visibility = View.VISIBLE
            binding.listMemberTitle.visibility = View.GONE
        } else {
            binding.addUploadLL.visibility = View.GONE
            binding.listMemberTitle.visibility = View.VISIBLE
        }

        binding.btnJoiningMeeting.setOnClickListener {
            val meetingService = ZoomSDK.getInstance().meetingService
            if (meetingService?.meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
                meetingService.returnToMeeting(this)
                ZoomSDK.getInstance().zoomUIService.hideMiniMeetingWindow()
            } else {
                if (Utils.isNetworkAvailable(this@MeetingJoiningActivity)) {
                    if (meetingHostId == hostID) {
                        if(isInMeeting){
                            onJoin("")
                        } else {
                            getMeetingZAKToken { success, token ->
                                if (success) {
                                    onStartMeeting(token)
                                }
                            }
                        }
                    } else {
                        if(isInMeeting){
                            onJoin("")
                        } else {
                            Toast.makeText(
                                this@MeetingJoiningActivity,
                                "The meeting is not yet started",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        this@MeetingJoiningActivity,
                        "Internet not available please try again later",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        binding.btnDeleteMeeting.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingJoiningActivity)) {
                deleteMeetingDialog()
            } else {
                Toast.makeText(this@MeetingJoiningActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }

        binding.addMemberEdit.setOnClickListener {
            if(Utils.isNetworkAvailable(this@MeetingJoiningActivity)) {
                showAddUsersDialog(this@MeetingJoiningActivity)
            } else {
                Toast.makeText(this@MeetingJoiningActivity, "Internet not available please try again later", Toast.LENGTH_LONG).show()
            }
        }

        binding.btnRefreshMeeting.setOnClickListener {
            showProgDialog()
            refreshMeetings { meetingDetails ->
                dismissProgDialog()
               // Log.e(TAG, "All meetings: $meetingDetails")
                if(meetingDetails != null) {
                    meetingTitle = meetingDetails.topicName!!
                    meetingAgenda = meetingDetails.agenda!!
                    meetingId = meetingDetails.meetingNumber!!
                    passCode = meetingDetails.meetingPasscode!!
                    joinURL = meetingDetails.joinURL!!
                    meetingHostId = meetingDetails.hostNumber!!
                    meetingStartTimestamp = meetingDetails.meetingStartTimestamp!!
                    isInMeeting = meetingDetails.inMeeting
                    MeetingUserDetails.setUsers(meetingDetails.members)
                    fetchMeetingDetails()
                }
            }
        }

        binding.btnMeetingAnalytics.setOnClickListener {
            startActivity(Intent(this@MeetingJoiningActivity, MeetingsAnalyticsActivity::class.java))
        }

        fetchMeetingDetails()
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

    fun refreshMeetings(onResult: (MeetingDetailsModel?) -> Unit) {
        try{
            db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS).document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS).document(meetingId)
                .get()
                .addOnSuccessListener { snapshot ->
                    Log.e(TAG, "All meetings: ${snapshot.data}")
                    val result = snapshot.toObject(MeetingDetailsModel::class.java)
                    onResult(result)
                    dismissProgDialog()
                }
                .addOnFailureListener {
                    Log.e(TAG, "Error fetching meetings: ${it.message}")
                    onResult(null)
                    dismissProgDialog()
                }
        }catch (e: Exception){
            onResult(null)
            e.printStackTrace()
        }

    }

    fun showAddUsersDialog(context: Context) {
        MaterialAlertDialogBuilder(
            ContextThemeWrapper(
                context,
                R.style.ThemeOverlay_Material3_MaterialAlertDialog
            )
        )
            .setTitle("Invite Users")
            .setItems(arrayOf("Add Users", "Upload Users", "Select Groups")) { _, which ->
                when (which) {
                    0 -> {
                        val bottomSheet = AddMeetingMemberBottomSheetFragment().apply {
                            arguments = Bundle().apply {
                                putString("adminId", hostID)
                            }
                        }
                        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                    }
                    1 -> openFilePicker()
                    2 -> {
                        val bottomSheet = SelectGroupBottomSheetFragment().apply {
                            arguments = Bundle().apply {
                                putString("adminId", hostID)
                            }
                        }
                        bottomSheet.show(supportFragmentManager, bottomSheet.tag)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()


    }
    // Function to launch the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/*"
        }
        fileSelectorLauncher.launch(intent)
    }
    private fun initZoomSDK(){
        try{
            val role = if(meetingHostId == hostID){
                "1"
            } else {
                "0"
            }
            getMeetingSignature(meetingId, role) { success, signature ->
                if (success) {
                    InitAuthSDKHelper.getInstance()
                        .initSDK(this@MeetingJoiningActivity, this@MeetingJoiningActivity, signature)
                }
            }
            if (null != ZoomSDK.getInstance().meetingService) {
                Log.e(TAG, "meetingService")
              //  ZoomSDK.getInstance().meetingService.addListener(this)
            } else {
                Log.e(TAG, "meetingService null")
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
    private fun fetchMeetingDetails(){
        try{
            userList = MeetingUserDetails.getUsers() as ArrayList<MembersZoomMeeting>
            GlobalMeetingListener.initialize(
                roomName = roomName,
                meetingId = meetingId,
                meetingTitle = meetingTitle,
                hostId = hostID,
                hostName = hostName,
                meetingStartTimestamp = meetingStartTimestamp,
            )
        }catch (e: Exception){
            e.printStackTrace()
        }
        setupAdapter()
    }
    private fun setupAdapter(){
        try{
            val isMeetingAdmin = meetingHostId == hostID
            userList.sortWith(compareByDescending<MembersZoomMeeting> { it.id == hostID }
                .thenBy { it.name.lowercase() })
            userAdapter = MeetingParticipantsAdapter(isMeetingAdmin, hostID, userList, this)
            binding.meetingUsersRv.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            binding.meetingUsersRv.adapter = userAdapter
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun removeMemberFromMeeting(meetingId: String, memberId: String, onComplete: (Boolean) -> Unit) {
        /*val meetingRef = db
            .collection(AppConstants.TABLE_MEETING_DETAILS)
            .document(meetingId)*/
        val meetingRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS).document(roomName).collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS).document(meetingId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(meetingRef)
            val meeting = snapshot.toObject(MeetingDetailsModel::class.java)
                ?: throw Exception("Meeting not found")

            val updatedMemberIds = meeting.memberIds.filter { it != memberId }
            val updatedMembers = meeting.members.filter { it.id != memberId }
            // 👆 assuming MembersZoomMeeting has `userId`
            transaction.update(meetingRef, mapOf(
                    "memberIds" to updatedMemberIds,
                    "members" to updatedMembers
                )
            )
        }
            .addOnSuccessListener {
                onComplete(true)
                dismissProgDialog()
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to remove member: ${it.message}")
                onComplete(false)
                dismissProgDialog()
            }
    }

    private fun deleteMeetingDialog() {
        try {
            android.app.AlertDialog.Builder(this)
                .setTitle("Delete Meeting Alert")
                .setMessage("Are you sure you want to delete this meeting?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteZoomMeeting {
                        if (it) {
                            deleteMeetingFireStore(meetingId) { isDeleted ->
                                if (isDeleted) {
                                    finish()
                                    dialogInterface.dismiss()
                                }
                            }
                        }
                    }
                }
                .setNegativeButton("No") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMeetingSignature(meetingId: String, role: String,onComplete: (Boolean, String) -> Unit) {
        try {
            showProgDialog()
            Log.e(TAG,"getMeetingSignature ::$appClientId $appClientSecret $meetingId")
            val request = SignatureRequest(meetingNumber = meetingId, role = role, appClientId,
                appClientSecret)
            RetrofitInstanceFunction.api.getSignature(request)
                .enqueue(object : Callback<SignatureResponse> {
                    override fun onResponse(call: Call<SignatureResponse>, response: Response<SignatureResponse>) {
                        if (response.isSuccessful) {
                            val signatureResponse = response.body()
                            if (signatureResponse?.signature != null) {
                                // Notification sent successfully
                                Log.e(TAG, "getMeetingSignature: ${signatureResponse.signature}")
                                onComplete(true, signatureResponse.signature)
                                dismissProgDialog()
                            }
                        } else {
                            // Handle error response
                            Log.e(TAG, "Error getMeetingSignature: ${response.errorBody()}")
                            onComplete(false, "")
                            Toast.makeText(this@MeetingJoiningActivity, "Failed to initiate the meeting", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                    }

                    override fun onFailure(call: Call<SignatureResponse>, t: Throwable) {
                        // Handle failure
                        Log.e(TAG, "Failed to getMeetingSignature: ${t.message}")
                        onComplete(false, "")
                        Toast.makeText(this@MeetingJoiningActivity, "Failed to initiate the meeting", Toast.LENGTH_SHORT).show()
                        dismissProgDialog()
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getMeetingZAKToken(onComplete: (Boolean, String) -> Unit) {
        try {
            showProgDialog()
            RetrofitInstanceFunction.api.getZAK(ZakRequest(accountUserEmail,
                serverClientId,
                serverClientSecret,
                accountId))
                .enqueue(object : Callback<ZakResponse> {
                    override fun onResponse(call: Call<ZakResponse>, response: Response<ZakResponse>) {
                        if (response.isSuccessful) {
                            val signatureResponse = response.body()
                            if (signatureResponse?.zak != null) {
                                // Notification sent successfully
                                Log.e(TAG, "getMeetingZAKToken: ${signatureResponse.zak}")
                                onComplete(true, signatureResponse.zak)
                                dismissProgDialog()
                            }
                        } else {
                            // Handle error response
                            Log.e(TAG, "Error on response getMeetingZAKToken: ${response.errorBody()}")
                            onComplete(false, "")
                            Toast.makeText(this@MeetingJoiningActivity, "Failed to initiate the meeting", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                    }

                    override fun onFailure(call: Call<ZakResponse>, t: Throwable) {
                        // Handle failure
                        Log.e(TAG, "Failed to getMeetingZAKToken: ${t.message}")
                        onComplete(false, "")
                        Toast.makeText(this@MeetingJoiningActivity, "Failed to initiate the meeting", Toast.LENGTH_SHORT).show()
                        dismissProgDialog()
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun deleteMeetingFireStore(meetingId: String, onComplete: (Boolean) -> Unit) {
        db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS).document(roomName).collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS).document(meetingId)
        //db.collection(AppConstants.TABLE_MEETING_DETAILS).document(meetingId)
            .delete()
            .addOnSuccessListener {
                onComplete(true)
                dismissProgDialog()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting user details", e)
                onComplete(false)
                dismissProgDialog()
            }
    }

    private fun deleteZoomMeeting(onComplete: (Boolean) -> Unit) {
        try {
            showProgDialog()
            val request = ZoomMeetingDeleteRequest(
                meetingId = meetingId,
                occurrenceId = occurrenceId,
                scheduleForReminder = true,
                cancelMeetingReminder = true,
                serverClientId,
                serverClientSecret,
                accountId
            )
            RetrofitInstanceFunction.api.deleteMeeting(request)
                .enqueue(object : Callback<ResponseBody> {
                    override fun onResponse(
                        call: Call<ResponseBody>,
                        response: Response<ResponseBody>
                    ) {
                        if (response.isSuccessful) {
                            val response = response.body()
                            if (response?.string() != null) {
                                // Notification sent successfully
                                Log.e(TAG, "delete Meeting: ${response.string()}")
                                Toast.makeText(
                                    this@MeetingJoiningActivity,
                                    "Meeting Deleted",
                                    Toast.LENGTH_SHORT
                                ).show()
                                onComplete(true)
                                dismissProgDialog()
                            }
                        } else {
                            // Handle error response
                            Log.e(TAG, "Error delete Meeting: ${response.errorBody()}")
                            Toast.makeText(
                                this@MeetingJoiningActivity,
                                "Failed to delete meeting",
                                Toast.LENGTH_SHORT
                            ).show()
                            onComplete(true)
                            dismissProgDialog()
                        }
                    }

                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                        // Handle failure
                        Log.e(TAG, "Failed to delete Meeting: ${t.message}")
                        Toast.makeText(
                            this@MeetingJoiningActivity,
                            "Failed to delete meeting",
                            Toast.LENGTH_SHORT
                        ).show()
                        onComplete(false)
                        dismissProgDialog()
                    }
                })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var handle: InMeetingNotificationHandle =
        InMeetingNotificationHandle { _: Context?, _: Intent? ->
            Log.e(TAG, "InMeetingNotificationHandle")
            true
        }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            Log.e(TAG,"KEYCODE_BACK")
            val meetingService = ZoomSDK.getInstance().meetingService
            if (meetingService?.meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING) {
                // 🔥 This minimizes Zoom UI (not your app)
             //   meetingService.returnToMeeting(this)
                // ✅ Move ONLY this activity to background
                //moveTaskToBack(true)
                //ZoomSDK.getInstance().zoomUIService.showMiniMeetingWindow()
                startActivity(Intent(this, HomeActivity::class.java))
               // ZoomSDK.getInstance().meetingService?.minimizeMeeting()
                //ZoomSDK.getInstance().zoomUIService.showMiniMeetingWindow()
                return true
            } else {
                finish()
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onZoomSDKInitializeResult(errorCode: Int, internalErrorCode: Int) {
        dismissProgDialog()
        Log.e(TAG, "onZoomSDKInitializeResult, errorCode=$errorCode, internalErrorCode=$internalErrorCode")
        if (errorCode != ZoomError.ZOOM_ERROR_SUCCESS) {
            Toast.makeText(this, "Failed to initialize Zoom Meeting. Error: $errorCode, internalErrorCode=$internalErrorCode", Toast.LENGTH_LONG).show()
        } else {
            ZoomSDK.getInstance().zoomUIService.setNewMeetingUI(CustomNewZoomUIActivity::class.java)
            ZoomSDK.getInstance().zoomUIService.disablePIPMode(false)
            ZoomSDK.getInstance().zoomUIService.enableMinimizeMeeting(true)
            ZoomSDK.getInstance().meetingSettingsHelper.enable720p(false)
            ZoomSDK.getInstance().meetingSettingsHelper.enableShowMyMeetingElapseTime(true)
            //ZoomSDK.getInstance().meetingService.addListener(this)
            ZoomSDK.getInstance().meetingSettingsHelper.setCustomizedNotificationData(null, handle)
            ZoomSDK.getInstance().meetingSettingsHelper.enableZoomDocsInCustomUI(true)
            ZoomSDK.getInstance().meetingSettingsHelper.setAutoConnectVoIPWhenJoinMeeting(
                ZoomSDK.getInstance().meetingSettingsHelper.isAutoConnectVoIPWhenJoinMeetingEnabled
            )
            Toast.makeText(this, "Initialize Zoom Meeting successfully.", Toast.LENGTH_LONG).show()
           // binding.btnJoiningMeeting.visibility = View.VISIBLE
            setMiniWindows()
            ZoomSDK.getInstance().meetingService?.removeListener(GlobalMeetingListener)
            ZoomSDK.getInstance().meetingService?.addListener(GlobalMeetingListener)

            if (mZoomSDK!!.tryAutoLoginZoom() == ZoomApiError.ZOOM_API_ERROR_SUCCESS) {
                UserLoginCallback.getInstance().addListener(this)
            }
        }
    }

    override fun onZoomSDKLoginResult(result: Long) {
        if (result.toInt() == ZoomAuthenticationError.ZOOM_AUTH_ERROR_SUCCESS) {
            finish()
        }
    }

    override fun onZoomSDKLogoutResult(result: Long) {
        //
    }

    override fun onZoomIdentityExpired() {
        Log.e(TAG, "onZoomIdentityExpired")
        if (mZoomSDK!!.isLoggedIn) {
            mZoomSDK!!.logoutZoom()
        }
    }

    override fun onZoomAuthIdentityExpired() {
        Log.e(TAG, "onZoomAuthIdentityExpired")
    }

    fun onJoin(joinToken: String) {
        if (!mZoomSDK!!.isInitialized) {
            Toast.makeText(this, "Initialize Zoom Meeting", Toast.LENGTH_SHORT).show()
            initZoomSDK()
            return
        }
        ZoomSDK.getInstance().smsService.enableZoomAuthRealNameMeetingUIShown(!ZoomSDK.getInstance().meetingSettingsHelper.isCustomizedMeetingUIEnabled)
        val params = JoinMeetingParams()
        params.meetingNo = meetingId
        params.displayName = hostName
        params.password = passCode
        params.join_token = joinToken
        //val options = JoinMeetingOptions()
        ZoomSDK.getInstance().meetingService
            .joinMeetingWithParams(this, params, ZoomMeetingUISettingHelper.getJoinMeetingOptions())
    }

    fun onStartMeeting(zak: String) {
        if (!mZoomSDK!!.isInitialized) {
            Toast.makeText(this, "Initialize Zoom Meeting", Toast.LENGTH_SHORT).show()
            initZoomSDK()
            return
        }
        val params = StartMeetingParamsWithoutLogin()
        params.meetingNo = meetingId
        params.displayName = hostName
        params.zoomAccessToken = zak   // ✅ ZAK ONLY HERE
        ZoomSDK.getInstance().meetingService
            .startMeetingWithParams(this, params, StartMeetingOptions())
    }

    override fun onResume() {
        super.onResume()
        Log.e(TAG, "onResume")
        isResumed = true
        isMeetingMinimized = false
        refreshUI()
        setMiniWindows()
    }

    private fun setMiniWindows() {
        Log.e(TAG,"afterMeetingMinimized")
        if (null != mZoomSDK && mZoomSDK!!.isInitialized && !mZoomSDK!!.meetingSettingsHelper.isCustomizedMeetingUIEnabled) {
            ZoomSDK.getInstance().zoomUIService
                .setZoomUIDelegate(object : SimpleZoomUIDelegate() {
                    override fun afterMeetingMinimized(activity: Activity) {
                        Log.e(TAG,"afterMeetingMinimized")
                        isMeetingMinimized = true
                       /* val intent = Intent(this@MeetingJoiningActivity, MeetingJoiningActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(intent)*/
                        startActivity(Intent(this@MeetingJoiningActivity, HomeActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                            }
                        )


                    }
                })
        }
    }

    override fun onPause() {
        super.onPause()
        Log.e(TAG, "onPause")
        isResumed = false
    }

    @SuppressLint("SetTextI18n")
    private fun refreshUI() {
        if (!ZoomSDK.getInstance().isInitialized) {
            binding.btnJoiningMeeting.text = if (meetingHostId == hostID) "Start Meeting" else "Join Meeting"
            return
        }
        val meetingStatus = ZoomSDK.getInstance().meetingService.meetingStatus
        if (meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING && isResumed) {
            binding.btnJoiningMeeting.text = "BackToMeeting"
            Log.e(TAG, "if isCustomizedMeetingUIEnabled")
        } else {
            Log.e(TAG, "else isCustomizedMeetingUIEnabled")
            binding.btnJoiningMeeting.text = if (meetingHostId == hostID) "Start Meeting" else "Join Meeting"
        }

    }

    override fun onMeetingParameterNotification(meetingParameter: MeetingParameter?) {
        Log.e(TAG, "onMeetingParameterNotification $meetingParameter")
    }

    override fun onMeetingStatusChanged(meetingStatus: MeetingStatus?, errorCode: Int, internalErrorCode: Int) {
        Log.e(TAG, "onMeetingStatusChanged $meetingStatus:$errorCode:$internalErrorCode")
        refreshUI()
        val currentDateTime = System.currentTimeMillis().toString()

        if (meetingHostId == hostID) {
            if(meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING){
                meetingStarted(currentDateTime)
            } else if(meetingStatus == MeetingStatus.MEETING_STATUS_ENDED){
                meetingEnded(currentDateTime)
            }
        } else {
            if(meetingStatus == MeetingStatus.MEETING_STATUS_INMEETING){
                participantJoined(currentDateTime)
            } else if(meetingStatus == MeetingStatus.MEETING_STATUS_ENDED){
                participantLeft(currentDateTime)
            }
        }
    }

    fun meetingStarted(currentDateTime: String) {
        try{
            //Log.e(TAG, Utils.getCurrentDate())
            meetingStartTimestamp = currentDateTime
            val startTimeRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                .document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                .document(meetingId)
            startTimeRef.update(mapOf(
                "meetingStartTimestamp" to currentDateTime,
                "inMeeting" to true))
                .addOnSuccessListener {
                    Log.e(TAG, "meetingStartTimestamp success")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }
            val analyticsRef = startTimeRef.collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
                .document(currentDateTime)

            val data = hashMapOf(
                "hostName" to hostName,
                "hostNumber" to hostID,
                "meetingNumber" to meetingId,
                "topicName" to meetingTitle,
                "startTime" to currentDateTime
            )
            analyticsRef.set(data, SetOptions.merge())
                .addOnSuccessListener {
                    participantJoined(currentDateTime)
                    val newMemberTokens = userList
                        .filter { it.id != hostID && it.fcm.isNotEmpty() }
                        .map { it.fcm }
                        .distinct()
                    val body = "$hostName stated this meeting"
                    sendNotification(meetingTitle, body, "MeetingStarted", newMemberTokens) { _ ->
                        //
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }

        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun meetingEnded(currentDateTime: String) {
        try{
            //Log.e(TAG, Utils.getCurrentDate())
            val endTimeRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                .document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                .document(meetingId)
            endTimeRef.update(mapOf(
                "meetingStartTimestamp" to "",
                "inMeeting" to false))
                .addOnSuccessListener {
                    Log.e(TAG, "meetingStartTimestamp success")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }
            val analyticsRef = endTimeRef.collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
                .document(meetingStartTimestamp)
            analyticsRef.update("endTime", currentDateTime)
                .addOnSuccessListener {
                   participantLeft(currentDateTime)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }

        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun participantJoined(currentDateTime: String) {
        try {
           // Log.e(TAG, Utils.getCurrentDate())
            val analyticsRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                .document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                .document(meetingId)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
                .document(meetingStartTimestamp)
            val participantRef = analyticsRef
                .collection("participants")
                .document(hostID)

            val joinData = hashMapOf(
                "userId" to hostID,
                "userName" to hostName,
                "joinTime" to currentDateTime
            )

            participantRef.set(joinData, SetOptions.merge())
                .addOnSuccessListener {
                    Log.e(TAG, "ParticipantJoined success")
                }

                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }

        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun participantLeft(currentDateTime: String) {
        try{
           // Log.e(TAG, Utils.getCurrentDate())
            val analyticsRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS)
                .document(roomName)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS)
                .document(meetingId)
                .collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_ANALYTICS)
                .document(meetingStartTimestamp)

            val participantRef = analyticsRef
                .collection("participants")
                .document(hostID)

            participantRef.update("leaveTime", currentDateTime)
                .addOnSuccessListener {
                    Log.e(TAG, "ParticipantLeft success")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    public override fun onDestroy() {
        super.onDestroy()
        MeetingUserDetails.clear()
        UserLoginCallback.getInstance().removeListener(this)
        InitAuthSDKHelper.getInstance().reset()
        if (null != ZoomSDK.getInstance().meetingService) {
            ZoomSDK.getInstance().meetingService.removeListener(this)
        }
        if (null != ZoomSDK.getInstance().meetingSettingsHelper) {
            ZoomSDK.getInstance().meetingSettingsHelper
                .setCustomizedNotificationData(null, null)
        }
    }

    override fun onUserSelectListener(users: MembersZoomMeeting) {
        deleteMemberDialog(users)
    }

    private fun deleteMemberDialog(users: MembersZoomMeeting){
        try{
            android.app.AlertDialog.Builder(this)
                .setTitle("Remove Member Alert")
                .setMessage("Are you sure you want to remove ${users.name}?")
                .setPositiveButton("Remove") { dialogInterface, _ ->
                    showProgDialog()
                    removeMemberFromMeeting(meetingId, users.id) {
                        if(it){
                            Toast.makeText(this, "Member removed successfully", Toast.LENGTH_SHORT).show()
                            MeetingUserDetails.removeUserById(users.id)
                            fetchMeetingDetails()
                        } else {
                            Toast.makeText(this, "Failed to remove member, try again.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    dialogInterface.dismiss()
                }
                /*.setNeutralButton("Block") { dialogInterface, _ ->
                    dialogInterface.dismiss()
                }*/
                .setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onAddMember(selectedUsersList: List<MembersGroup>) {
        if (selectedUsersList.isEmpty()) {
            Toast.makeText(this, "Select at least one member", Toast.LENGTH_SHORT).show()
            return
        }
        showProgDialog()
        val meetingRef = db.collection(AppConstants.TABLE_ZOOM_MEETING_DETAILS).document(roomName).collection(AppConstants.TABLE_ZOOM_ROOM_MEETING_DETAILS).document(meetingId)

        db.runTransaction { transaction ->
            val snapshot = transaction.get(meetingRef)
            val meeting = snapshot.toObject(MeetingDetailsModel::class.java)
                ?: throw Exception("Meeting not found")

            val existingIds = meeting.memberIds.toMutableSet()
            val existingMembers = meeting.members.toMutableList()

            selectedUsersList.forEach { user ->
                if (!existingIds.contains(user.id)) {
                    existingIds.add(user.id)
                    existingMembers.add(
                        MembersZoomMeeting(
                            id = user.id,
                            name = user.name,
                            fcm = user.fcmToken
                        )
                    )
                    MeetingUserDetails.addUser(
                        MembersZoomMeeting(
                            id = user.id,
                            name = user.name,
                            fcm = user.fcmToken
                        )
                    )
                }
            }

            transaction.update(
                meetingRef,
                mapOf(
                    "memberIds" to existingIds.toList(),
                    "members" to existingMembers
                )
            )
        }
            .addOnSuccessListener {
                Log.e(TAG, "Members added successfully")
                selectedUsersList.forEach { user ->
                   val members = MembersZoomMeeting(
                        id = user.id,
                        name = user.name,
                        fcm = user.fcmToken
                    )
                    if (!MeetingUserDetails.getUsers().contains(members)) {
                        MeetingUserDetails.addUser(
                            MembersZoomMeeting(
                                id = user.id,
                                name = user.name,
                                fcm = user.fcmToken
                            )
                        )
                    }
                }
                val newMemberTokens = selectedUsersList
                    .filter { it.id != hostID && it.fcmToken.isNotEmpty() }
                    .map { it.fcmToken }
                    .distinct()
                val body = "$hostName invited you to join meeting"
                sendNotification(meetingTitle, body, "NewMeeting", newMemberTokens) { _ ->
                    //if (isSent) {
                        fetchMeetingDetails()
                        Toast.makeText(this, "Members added successfully", Toast.LENGTH_SHORT).show()
                   // }
                }
            }
            .addOnFailureListener {
                dismissProgDialog()
                Log.e(TAG, "Failed to add members: ${it.message}")
                Toast.makeText(this, "Failed to add members", Toast.LENGTH_SHORT).show()

            }
    }

    private fun sendNotification(title: String, body: String, notificationTitle: String, tokens: List<String>, onComplete: (Boolean) -> Unit) {
        val senderName = sharedPref.getString(AppConstants.USER_NAME, null)
        val senderID = sharedPref.getString(AppConstants.USER_ID, null)

        val notificationRequest = NotificationRequest(
            tokens = tokens,  // Replace with the actual FCM token
            title = title,
            message = body,
            customData = mapOf(
                AppConstants.COMMUNITY_NOTIFICATION to notificationTitle,
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
            val existingIds = userList.map { it.id }.toSet()
            val addedIds = mutableSetOf<String>()
            usersFromFileList.clear()
            GlobalScope.launch {
                withContext(Dispatchers.IO) {
                    Log.i(TAG, "doInBackground: Importing...")
                    runOnUiThread {
                        Toast.makeText(this@MeetingJoiningActivity, "Importing...", Toast.LENGTH_SHORT).show()
                    }
                    val readExcelNew: List<Map<Int, Any>> = ExcelUtil.readExcelNew(this@MeetingJoiningActivity, uri, uri.path)
                    Log.i(TAG, "onActivityResult:readExcelNew: ${ readExcelNew.size} ")
                    if (readExcelNew.isNotEmpty()) {
                        Log.i(TAG, "run: successfully imported")
                        runOnUiThread {
                            Toast.makeText(this@MeetingJoiningActivity, "successfully imported", Toast.LENGTH_SHORT).show()
                        }

                        for (i in readExcelNew.indices) {
                            val map = readExcelNew[i]
                            val name = map[0]?.toString() ?: ""
                            //val email = map[1]?.toString() ?: ""
                            //val country = map[2]?.toString() ?: ""
                            val phoneNumber = map[3]?.toString() ?: ""
                            //val category = map[4]?.toString() ?: ""

                            if (name.isNotEmpty() && name != "Name" && phoneNumber.isNotEmpty()) {
                                /*val user = MembersGroup(phoneNumber, name, phoneNumber, false, "")
                                val existsInUserList = userList.any { it.id == user.id }
                                val existsInFileList = usersFromFileList.any { it.id == user.id }
                                if (!existsInUserList && !existsInFileList) {
                                    usersFromFileList.add(user)
                                }*/
                                if (phoneNumber !in existingIds && phoneNumber !in addedIds) {
                                    usersFromFileList.add(
                                        MembersGroup(
                                            id = phoneNumber,
                                            name = name,
                                            phoneNumber = phoneNumber,
                                            fcmToken = ""
                                        )
                                    )

                                    addedIds.add(phoneNumber)
                                }
                            }
                        }
                        if(usersFromFileList.isNotEmpty()){
                            Log.e(TAG, " Users list:: ${usersFromFileList.size}")
                            onAddMember(usersFromFileList)
                        }
                    } else {
                        runOnUiThread {
                            Toast.makeText(this@MeetingJoiningActivity, "No data available", Toast.LENGTH_SHORT).show()
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
}