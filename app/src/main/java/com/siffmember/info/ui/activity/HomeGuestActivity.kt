package com.siffmember.info.ui.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.*
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityHomeBinding
import com.siffmember.info.ui.adapter.AnnouncementAdapter
import com.siffmember.info.ui.adapter.GridSpacingItemDecoration
import com.siffmember.info.ui.adapter.HomeMenuAdapter
import com.siffmember.info.ui.fragment.SocialMediaBottomSheetFragment
import com.siffmember.info.ui.fragment.WhatsAppGroupBottomSheetFragment
import com.siffmember.info.ui.model.AnnouncementDetails
import com.siffmember.info.ui.model.MenuDetails
import com.siffmember.info.ui.viewmodel.NotificationEventBus
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.OpenPoints
import kotlinx.coroutines.launch

class HomeGuestActivity : BaseActivity(), HomeMenuAdapter.HomeMenuListener, SocialMediaBottomSheetFragment.SocialMediaBottomSheetListener, AnnouncementAdapter.DeleteListener {

    companion object {
        var TAG = "HomeGuestActivity"
    }

    private lateinit var binding: ActivityHomeBinding
    private lateinit var db: FirebaseFirestore
    private var announcementList = mutableListOf<AnnouncementDetails>()
    private var announcementAdapter: AnnouncementAdapter? = null
    private var category = "guest"

    @SuppressLint("SetTextI18n")
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
        db = Firebase.firestore
        OpenPoints.setIsGuestUser(true)
        binding.apply {
            userName.text = "Hi! Guest User"
            logout.visibility = View.GONE
            addAnnounce.visibility = View.GONE
            refresh.visibility = View.GONE
        }
        checkAndRequestNotificationPermission()
        lifecycleScope.launch {
            NotificationEventBus.events.collect { _ ->
                getAllAnnouncements()
            }
        }
        clearAllNotifications()
        setMenuAdapter()
        getAllAnnouncements()
    }

    private fun setMenuAdapter() {
        try {
            // Sample data for the grid
            val items = listOf(
                MenuDetails(resources.getString(R.string.education_title), R.drawable.ic_education),
                MenuDetails(resources.getString(R.string.contact_title), R.drawable.ic_contact_us),
                MenuDetails(resources.getString(R.string.social_title), R.drawable.ic_social_media_home1)
            )
            val isTablet = resources.configuration.smallestScreenWidthDp >= 600

            var spanCount = 3
            val currentOrientation = resources.configuration.orientation
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                // Activity is launched in Portrait mode for the first time
                spanCount = if (isTablet) {
                    4
                } else {
                    3
                }
            } else if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                // Activity is launched in Landscape mode for the first time
                spanCount = if (isTablet) {
                    7
                } else {
                    6
                }
            }
            val adapter = HomeMenuAdapter(items, this)
            binding.menuList.layoutManager = GridLayoutManager(this, spanCount) // Dynamic columns
            // Define spacing between grid items (in pixels)
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
                startActivity(Intent(this, EducationGuestActivity::class.java))
            }
            resources.getString(R.string.contact_title) -> {
                startActivity(Intent(this, ContactUsActivity::class.java))
            }
            resources.getString(R.string.social_title) -> {
                try {
                    val bottomSheetFragment = SocialMediaBottomSheetFragment()
                    bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showToast("Notification permission granted")
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

    private fun getAllAnnouncements(){
        try{
            Log.e(TAG, "getAllAnnouncements")
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
                        Toast.makeText(this@HomeGuestActivity,"Announcement not available.", Toast.LENGTH_LONG).show()
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


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KEYCODE_BACK) {
            OpenPoints.setIsGuestUser(false)
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDeleteDevice(announceDetails: AnnouncementDetails?) {
        //
    }
}