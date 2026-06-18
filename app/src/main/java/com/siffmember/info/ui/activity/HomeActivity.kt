package com.siffmember.info.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject
import com.google.firebase.firestore.toObjects
import com.siffmember.info.R
import com.siffmember.info.data.remote.api.RetrofitInstanceFunction
import com.siffmember.info.data.remote.model.functions.NotificationRequest
import com.siffmember.info.data.remote.model.functions.NotificationResponse
import com.siffmember.info.databinding.ActivityHomeBinding
import com.siffmember.info.ui.adapter.AnnouncementAdapter
import com.siffmember.info.ui.adapter.GridSpacingItemDecoration
import com.siffmember.info.ui.adapter.HomeMenuAdapter
import com.siffmember.info.ui.fragment.AnnounceBottomSheetFragment
import com.siffmember.info.ui.fragment.SocialMediaBottomSheetFragment
import com.siffmember.info.ui.fragment.WhatsAppGroupBottomSheetFragment
import com.siffmember.info.ui.model.AnnouncementDetails
import com.siffmember.info.ui.model.GetAppVersion
import com.siffmember.info.ui.model.MenuDetails
import com.siffmember.info.ui.model.Users
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.NotificationEventBus
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat
import com.siffmember.info.utils.OpenPoints
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.siffmember.info.BuildConfig
import com.siffmember.info.data.local.entity.UserDetailsEntity
import com.siffmember.info.socialFeeds.SocialFeedHomeActivity
import com.siffmember.info.ui.viewmodel.UserDetailsViewModel
import com.siffmember.info.utils.CallLogDetails
import com.siffmember.info.utils.PermissionUtils
import com.siffmember.info.utils.UsersDetails

class HomeActivity : BaseActivity(), AnnouncementAdapter.DeleteListener, AnnounceBottomSheetFragment.BottomSheetListener, HomeMenuAdapter.HomeMenuListener, SocialMediaBottomSheetFragment.SocialMediaBottomSheetListener {

    companion object {
        var TAG = "HomeActivity"
    }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var db: FirebaseFirestore
    private var announcementAdapter: AnnouncementAdapter? = null
    private lateinit var userDetailViewModel: UserDetailsViewModel
    private lateinit var viewModel: CommunityViewModel
    private lateinit var postsViewModel: PostsMessageViewModel
    private var announcementList = mutableListOf<AnnouncementDetails>()
    private var category = ""
    private var isAnnouncement = false
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater).apply {
            setContentView(root)
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // When keyboard opens -> push bottom layout up
            binding.homeSv.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                //var allGranted = true
                permissions.entries.forEach { entry ->
                    val permission = entry.key
                    val isGranted = entry.value
                    if (!isGranted) {
                        //allGranted = false
                        if (PermissionUtils.isPermissionDeniedForever(this, permission)) {
                            showGoToSettingsDialog()
                        } else {
                            showPermissionDeniedDialog()
                        }
                    }
                }
                /*if (allGranted) {
                    onAllPermissionsGranted()
                }*/
            }
        userDetailViewModel = ViewModelProvider(this)[UserDetailsViewModel::class.java]
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        db = Firebase.firestore

        CommunityChat.setIsChatOpen(false)
        OpenPoints.setIsGuestUser(false)
        CallLogDetails.setCallInitiated(false)
        CallLogDetails.setUserName("")
        CallLogDetails.setUserPhoneNumber("")
        try {
            val user = Firebase.auth.currentUser
            if(user != null){
                val userId = sharedPref.getString(AppConstants.USER_ID, null)
                userDetailViewModel.getUserDetails(userId!!).observe(this) {
                    if (it != null) {
                        Log.e(TAG, "Get user document from local")
                        usersMenuSetup(Users(it.name, it.email_id, it.country, it.phone_number, it.category), it.phone_number)
                    } else {
                        getUserDetails(userId)
                    }
                }
                getAllAnnouncements()
                getAppLatestVersion()
            }
            val inputStream = resources.openRawResource(R.raw.client_secret_web)
            val json = inputStream.bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val apiKey = jsonObject.getJSONObject("web").getString("api_key")
            val driveFolderId = jsonObject.getJSONObject("web").getString("drive_folder_id")
            OpenPoints.setApiKey(apiKey)
            OpenPoints.setDriveFolderId(driveFolderId)

        }catch (e: Exception){
            e.printStackTrace()
        }
        isAnnouncement = false
        binding.apply {

            refresh.setOnClickListener {
                try{
                    val user = Firebase.auth.currentUser
                    if(user != null){
                        val userId = sharedPref.getString(AppConstants.USER_ID, null)
                        getUserDetails(userId!!)
                    }
                    getAllAnnouncements()
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            logout.setOnClickListener {
                isAnnouncement = false
                startActivity(Intent(this@HomeActivity, ProfileDetailsActivity::class.java))
            }

            addAnnounce.setOnClickListener {
                isAnnouncement = true
                val bottomSheetFragment = AnnounceBottomSheetFragment()
                bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
            }
        }
        checkAndRequestNotificationPermission()
        lifecycleScope.launch {
            NotificationEventBus.events.collect { _ ->
                getAllAnnouncements()
            }
        }
        clearAllNotifications()

    }

    private fun requestPermissions() {
        if (!PermissionUtils.hasAllPermissions(this)) {
            permissionLauncher.launch(PermissionUtils.REQUIRED_PERMISSIONS)
        } /*else {
            onAllPermissionsGranted()
        }*/
    }
    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("These permissions are required for call monitoring. Please allow them.")
            .setPositiveButton("Retry") { _, _ ->
                requestPermissions()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showGoToSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("You have permanently denied permissions. Please enable them in Settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.fromParts("package", packageName, null)
                startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    private fun setMenuAdapter() {
        try {
            // Sample data for the grid
            var items: List<MenuDetails>
            when (category) {
                "Admin" -> {
                    items = listOf(
                        MenuDetails(resources.getString(R.string.education_title), R.drawable.ic_education),
                        MenuDetails(resources.getString(R.string.open_points), R.drawable.ic_open_points),
                        MenuDetails(resources.getString(R.string.member_search), R.drawable.ic_member_search),
                        MenuDetails(resources.getString(R.string.add_user), R.drawable.ic_add_user),
                       // MenuDetails(resources.getString(R.string.appointment_title), R.drawable.ic_appointment),
                        MenuDetails(resources.getString(R.string.community_title), R.drawable.ic_community),
                        MenuDetails(resources.getString(R.string.contact_title), R.drawable.ic_contact_us),
                        MenuDetails(resources.getString(R.string.social_title), R.drawable.ic_social_media_home1),
                        MenuDetails(resources.getString(R.string.guest_education_title), R.drawable.ic_education),
                        MenuDetails(resources.getString(R.string.meetings_title), R.drawable.ic_meetings),
                        MenuDetails(resources.getString(R.string.chat_with_us_title), R.drawable.ic_chatai),
                        MenuDetails(resources.getString(R.string.social_post), R.drawable.ic_stream)
                    )
                }
                "UserL1" -> {
                    items = listOf(
                        MenuDetails(resources.getString(R.string.education_title), R.drawable.ic_education),
                        MenuDetails(resources.getString(R.string.open_points), R.drawable.ic_open_points),
                        MenuDetails(resources.getString(R.string.member_search), R.drawable.ic_member_search),
                        // MenuDetails(resources.getString(R.string.appointment_title), R.drawable.ic_appointment),
                        MenuDetails(resources.getString(R.string.community_title), R.drawable.ic_community),
                        MenuDetails(resources.getString(R.string.contact_title), R.drawable.ic_contact_us),
                        MenuDetails(resources.getString(R.string.social_title), R.drawable.ic_social_media_home1),
                        MenuDetails(resources.getString(R.string.meetings_title), R.drawable.ic_meetings),
                        //MenuDetails(resources.getString(R.string.chat_with_us_title), R.drawable.ic_chatai)
                        MenuDetails(resources.getString(R.string.social_post), R.drawable.ic_stream)

                    )
                }
                "UserL2" -> {
                    items = listOf(
                        MenuDetails(resources.getString(R.string.education_title), R.drawable.ic_education),
                        MenuDetails(resources.getString(R.string.open_points), R.drawable.ic_open_points),
                        MenuDetails(resources.getString(R.string.member_search), R.drawable.ic_member_search),
                        // MenuDetails(resources.getString(R.string.appointment_title), R.drawable.ic_appointment),
                        MenuDetails(resources.getString(R.string.community_title), R.drawable.ic_community),
                        MenuDetails(resources.getString(R.string.contact_title), R.drawable.ic_contact_us),
                        MenuDetails(resources.getString(R.string.social_title), R.drawable.ic_social_media_home1),
                        MenuDetails(resources.getString(R.string.meetings_title), R.drawable.ic_meetings),
                        //MenuDetails(resources.getString(R.string.chat_with_us_title), R.drawable.ic_chatai)

                    )
                }
                else -> {
                    items = listOf(
                        MenuDetails(resources.getString(R.string.education_title), R.drawable.ic_education),
                        // MenuDetails(resources.getString(R.string.appointment_title), R.drawable.ic_appointment),
                        MenuDetails(resources.getString(R.string.community_title), R.drawable.ic_community),
                        MenuDetails(resources.getString(R.string.contact_title), R.drawable.ic_contact_us),
                        MenuDetails(resources.getString(R.string.social_title), R.drawable.ic_social_media_home1),
                        MenuDetails(resources.getString(R.string.meetings_title), R.drawable.ic_meetings),
                        //MenuDetails(resources.getString(R.string.chat_with_us_title), R.drawable.ic_chatai)
                    )
                }
            }
            //val isTablet = resources.getBoolean(R.bool.isTablet)
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600

            Log.e(TAG,"$isTablet")
            //Log.e(TAG,"$isTablets")
            var spanCount = 3
            val currentOrientation = resources.configuration.orientation
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                // Activity is launched in Portrait mode for the first time
                //spanCount = 3
                spanCount = if (isTablet) {
                    4
                } else {
                    3
                }
            } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Activity is launched in Landscape mode for the first time
                //spanCount = 6
                spanCount = if (isTablet) {
                    7
                } else {
                    6
                }
            }
            val adapter = HomeMenuAdapter(items, this)
            binding.menuList.layoutManager = GridLayoutManager(this, spanCount) // Dynamic columns
            // Define spacing between grid items (in pixels)
           // val spacing = resources.getDimensionPixelSize(R.dimen.margin3)
            val spacing = 5
            val itemDecoration = GridSpacingItemDecoration(spanCount, spacing, true)
            binding.menuList.addItemDecoration(itemDecoration)
            binding.menuList.adapter = adapter
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMenuSelectListener(menuDetails: MenuDetails) {
        when (menuDetails.title) {
            resources.getString(R.string.education_title) -> {
                OpenPoints.setIsGuestUser(false)
                isAnnouncement = false
                startActivity(Intent(this, EducationActivity::class.java))
            }
            resources.getString(R.string.open_points) -> {
                isAnnouncement = false
                startActivity(Intent(this, OpenPointsListActivity::class.java))
            }
            resources.getString(R.string.member_search) -> {
                isAnnouncement = false
                //startActivity(Intent(this, MemberSearchActivity::class.java))
                startActivity(Intent(this, MembershipSearchActivity::class.java))
            }
            resources.getString(R.string.add_user) -> {
                isAnnouncement = false
                startActivity(Intent(this, AddUserActivity::class.java))
            }
            resources.getString(R.string.appointment_title) -> {
                isAnnouncement = false
                //startActivity(Intent(this, AppointmentActivity::class.java))
            }
            resources.getString(R.string.community_title) -> {
                isAnnouncement = false
                startActivity(Intent(this, CommunityMainActivity::class.java))
            }
            resources.getString(R.string.contact_title) -> {
                isAnnouncement = false
                startActivity(Intent(this, ContactUsActivity::class.java))
            }
            resources.getString(R.string.social_title) -> {
                isAnnouncement = false
                try {
                    val bottomSheetFragment = SocialMediaBottomSheetFragment()
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            resources.getString(R.string.guest_education_title) -> {
                isAnnouncement = false
                startActivity(Intent(this, EducationGuestActivity::class.java))
            }
            resources.getString(R.string.meetings_title) -> {
                isAnnouncement = false
                startActivity(Intent(this, MeetingsHomeActivity::class.java))
            }
            resources.getString(R.string.chat_with_us_title) -> {
                isAnnouncement = false
                startActivity(Intent(this, AITribeOneHomeActivity::class.java))
            }
            resources.getString(R.string.social_post) -> {
                isAnnouncement = false
                startActivity(Intent(this, SocialFeedHomeActivity::class.java))
            }
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
               // showToast("Notification permission granted")
                requestPermissions()
            } else {
                showToast("Notification permission denied")
            }
        }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13+
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    // Permission already granted
                   // showToast("Notification permission already granted")
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // Explain why the permission is needed before requesting it
                    showToast("Notification permission is required to show alerts")
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Directly request the permission
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupAdapter(){
        try{
            announcementAdapter = AnnouncementAdapter(announcementList, this, category)
            binding.recycler.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
            binding.recycler.adapter = announcementAdapter

            binding.arIndicator.removeIndicators()
            binding.arIndicator.attachTo(binding.recycler, true)
            binding.recycler.post {
                if(announcementList.isNotEmpty()) {
                    binding.recycler.smoothScrollToPosition(0)
                    binding.arIndicator.numberOfIndicators = announcementList.size
                    binding.arIndicator.setSelectedPosition(0)
                }
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun getAppLatestVersion(){
        try {
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_APP_VERSION).document(AppConstants.TABLE_APP_VERSION_ID)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.data != null) {
                        val user = document.toObject<GetAppVersion>()
                        if (user != null) {
                            val serverVersion = user.versionName_android
                            val currentVersion = getCurrentVersionName()
                            if (isUpdateRequired(currentVersion, serverVersion!!)) {
                                updateAppVersion()
                            } else {
                                Log.e(TAG, "You are using the latest version")
                            }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("getUserDetails", "get failed with ", exception)
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    fun isUpdateRequired(current: String, server: String): Boolean {
        val currentParts = current.split(".")
        val serverParts = server.split(".")

        val maxLength = maxOf(currentParts.size, serverParts.size)

        for (i in 0 until maxLength) {
            val c = currentParts.getOrNull(i)?.toIntOrNull() ?: 0
            val s = serverParts.getOrNull(i)?.toIntOrNull() ?: 0

            if (s > c) return true   // server version is higher
            if (s < c) return false  // current version is higher
        }
        return false
    }


//    private fun getCurrentVersionName(): String{
//        val info = packageManager.getPackageInfo(packageName, 0)
//        val versionName = info.versionName
//        return versionName!!
//    }

    private fun getCurrentVersionName(): String {
        return BuildConfig.VERSION_NAME
    }

    fun openPlayStore() {
        val appPackageName = packageName // your own app package
        try {
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri()))
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$appPackageName".toUri()))
        }
    }

    fun updateAppVersion(){
        try {
            AlertDialog.Builder(this)
                .setTitle("Update Available")
                .setMessage("A new version of the app is available. Please update from Play Store.")
                .setPositiveButton("Update") { dialogInterface, _ ->
                    openPlayStore()
                    dialogInterface.dismiss()
                }
                .setNegativeButton("Cancel"){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    @SuppressLint("SetTextI18n")
    private fun getUserDetails(phoneNumber: String){
        try{
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_USER_DETAILS).document(phoneNumber)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.data != null) {
                        val user = document.toObject<Users>()
                        if(user != null){
                            Log.e(TAG, "Get user document from server")
                            lifecycleScope.launch(Dispatchers.IO) {
                                userDetailViewModel.insertUserDetails(UserDetailsEntity(user.phone_number!!,user.name!!,user.email_id!!,user.country!!,user.category!!))
                            }
                            usersMenuSetup(user, phoneNumber)
                        } else {
                            Log.e(TAG, "No such document")
                            FirebaseAuth.getInstance().signOut()
                            sharedPrefEditor.clear()
                            sharedPrefEditor.apply()
                            viewModel.deleteAllCommunities()
                            viewModel.deleteAllCategoryTag()
                            postsViewModel.deleteAllPostMessages()
                            postsViewModel.deleteAllReplyPostMessages()
                            userDetailViewModel.deleteUserDetails()
                            startActivity(Intent(this@HomeActivity, IntroActivity::class.java))
                            finishAffinity()
                        }
                    }
                    dismissProgDialog()
                }
                .addOnFailureListener { exception ->
                    Log.e("getUserDetails", "get failed with ", exception)
                    dismissProgDialog()
                }

        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private fun usersMenuSetup(user: Users, phoneNumber: String){
        try{
            runOnUiThread {
                binding.userName.text = "Hi! ${user.name}"
                sharedPrefEditor.putString(AppConstants.USER_NAME, "${user.name}").commit()
                sharedPrefEditor.putString(AppConstants.USER_ID, phoneNumber).commit()
                sharedPrefEditor.putString(AppConstants.USER_EMAIL, user.email_id).commit()
                sharedPrefEditor.putString(AppConstants.USER_CATEGORY,  user.category!!).commit()
                category = user.category
                when (user.category) {
                    "Admin" -> {
                        sharedPrefEditor.putBoolean(AppConstants.IS_EDIT_ACCESS, true).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_ADMIN, true).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MENTOR, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MEMBER, false).commit()
                        binding.addAnnounce.visibility = View.VISIBLE
                    }
                    "UserL1" -> {
                        sharedPrefEditor.putBoolean(AppConstants.IS_EDIT_ACCESS, true).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_ADMIN, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MENTOR, true).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MEMBER, false).commit()
                        binding.addAnnounce.visibility = View.VISIBLE
                    }
                    "UserL2" -> {
                        sharedPrefEditor.putBoolean(AppConstants.IS_EDIT_ACCESS, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_ADMIN, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MENTOR, true).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MEMBER, false).commit()
                        binding.addAnnounce.visibility = View.GONE
                    }
                    "MemberL1" -> {
                        sharedPrefEditor.putBoolean(AppConstants.IS_EDIT_ACCESS, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_ADMIN, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MENTOR, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MEMBER, true).commit()
                        binding.addAnnounce.visibility = View.GONE
                    }
                    "MemberL2" -> {
                        sharedPrefEditor.putBoolean(AppConstants.IS_EDIT_ACCESS, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_ADMIN, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MENTOR, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MEMBER, true).commit()
                        binding.addAnnounce.visibility = View.GONE
                    }
                    "MemberL3" -> {
                        sharedPrefEditor.putBoolean(AppConstants.IS_EDIT_ACCESS, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_ADMIN, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MENTOR, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MEMBER, true).commit()
                        binding.addAnnounce.visibility = View.GONE
                    }
                    else -> {
                        sharedPrefEditor.putBoolean(AppConstants.IS_EDIT_ACCESS, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_ADMIN, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MENTOR, false).commit()
                        sharedPrefEditor.putBoolean(AppConstants.IS_MEMBER, true).commit()
                        binding.addAnnounce.visibility = View.GONE
                    }
                }
                setMenuAdapter()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun getAllAnnouncements(){
        try{
            showProgDialog()
            announcementList.clear()
            val docRef = db.collection(AppConstants.TABLE_ANNOUNCEMENT_DETAILS)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        if(document.size() == 0){
                            binding.announceCardView.visibility = View.GONE
                            binding.noAnnounce.visibility = View.VISIBLE
                        } else {
                            binding.announceCardView.visibility = View.VISIBLE
                            binding.noAnnounce.visibility = View.GONE
                            val announceDetail = document.toObjects<AnnouncementDetails>()
                            val sortedAnnouncements = sortAnnouncementsByDate(announceDetail)
                            sortedAnnouncements.forEach {
                                announcementList.add(it)
                            }
                            setupAdapter()
                        }
                        dismissProgDialog()
                    } else {
                        Log.e(TAG, "No such document")
                        Toast.makeText(this@HomeActivity,"Announcement not available.", Toast.LENGTH_LONG).show()
                        dismissProgDialog()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "get failed with ", exception)
                    dismissProgDialog()
                }
        } catch(e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    private fun sortAnnouncementsByDate(announcements: List<AnnouncementDetails>): List<AnnouncementDetails> {
        return announcements.sortedByDescending { announcement ->
            announcement.dateTime?.toLongOrNull() ?: 0L // Convert to Long, default to 0 if null
        }
    }

    override fun onDeleteDevice(announceDetails: AnnouncementDetails?) {
        UsersDetails.setAnnouncementDetail(announceDetails!!)
        startActivity(Intent(this, AnnouncementDetailActivity::class.java))
    }


    override fun onAddAnnouncement(title: String, description: String) {
        showProgDialog()
        addAnnouncement(title, description)
    }

    private fun addAnnouncement(title: String, description: String){
        try{
            val currentTimestamp = System.currentTimeMillis()
            val announceData = AnnouncementDetails("$currentTimestamp", title, description)
            val docRef = db.collection(AppConstants.TABLE_ANNOUNCEMENT_DETAILS).document("$currentTimestamp")
            docRef.set(announceData)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    dismissProgDialog()
                    Toast.makeText(this@HomeActivity,"Announcement added successfully", Toast.LENGTH_LONG).show()
                    getAllAnnouncements()
                    sendNotificationsAnnouncement(title, description)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@HomeActivity,"Failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    private fun sendNotification(title: String, body: String, topic: String? = null) {
        val senderID = sharedPref.getString(AppConstants.USER_ID, null)
        val notificationRequest = NotificationRequest(
            topic = topic,  // Replace with the actual FCM token
            title = title,
            message = body,
            customData = mapOf("senderId" to senderID.toString())
        )
        RetrofitInstanceFunction.api.sendNotification(notificationRequest).enqueue(object : Callback<NotificationResponse> {
            override fun onResponse(call: Call<NotificationResponse>, response: Response<NotificationResponse>) {
                if (response.isSuccessful) {
                    val notificationResponse = response.body()
                    if (notificationResponse?.success != null) {
                        // Notification sent successfully
                        Log.e(TAG,"Notification sent: ${notificationResponse.success}")
                    }
                } else {
                    // Handle error response
                    Log.e(TAG,"Error sending notification: ${response.errorBody()}")
                }
            }

            override fun onFailure(call: Call<NotificationResponse>, t: Throwable) {
                // Handle failure
                Log.e(TAG,"Failed to send notification: ${t.message}")
            }
        })
    }

    private fun sendNotificationsAnnouncement(title: String, body: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                async {
                    sendNotification(title, body, topic = AppConstants.ANNOUNCEMENT_TOPIC)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in sendNotificationsToUsers", e)
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            finishAffinity()
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun clearAllNotifications() {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancelAll()
    }

    override fun onOpenWhatsApp() {
        try {
            val bottomSheetFragment = WhatsAppGroupBottomSheetFragment()
            bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
        }catch (e: Exception){
            e.printStackTrace()
        }
    }
}