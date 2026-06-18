package com.siffmember.info.ui.activity

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityAnnouncementDetailBinding
import com.siffmember.info.ui.model.AnnouncementDetails
import com.siffmember.info.ui.viewmodel.NotificationEventBus
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.UsersDetails
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class AnnouncementDetailActivity : BaseActivity() {

    companion object {
        var TAG = "AnnouncementDetailActivity"
    }
    private lateinit var binding: ActivityAnnouncementDetailBinding
    private lateinit var db: FirebaseFirestore
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAnnouncementDetailBinding.inflate(layoutInflater).apply {
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
            binding.announceScroll.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }

        db = Firebase.firestore
        binding.btnDelete.visibility = if (sharedPref.getBoolean(AppConstants.IS_ADMIN, false)) View.VISIBLE else View.GONE
        val announceDetails = UsersDetails.getAnnouncementDetail()
        binding.announceTitle.text = announceDetails.title
        binding.announceDescription.text = announceDetails.description
        binding.btnDelete.setOnClickListener {
            deleteAnnouncementDialog(announceDetails)
        }
    }

    private fun deleteAnnouncementDialog(announceDetails: AnnouncementDetails?){
        try{
            AlertDialog.Builder(this)
                .setTitle("Delete Announcement Alert")
                .setMessage("Are you sure you want to delete this announcement?")
                .setPositiveButton("Yes") { dialogInterface, _ ->
                    deleteAnnouncement(announceDetails)
                    dialogInterface.dismiss()
                }
                .setNegativeButton("No"){ dialogInterface, _ ->
                    dialogInterface.dismiss()
                }
                .create()
                .show()
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun deleteAnnouncement(announceDetails: AnnouncementDetails?){
        showProgDialog()
        db.collection(AppConstants.TABLE_ANNOUNCEMENT_DETAILS).document(announceDetails!!.dateTime!!)
            .delete()
            .addOnSuccessListener {
                GlobalScope.launch {
                    NotificationEventBus.sendEvent("New Announcement")
                }
                Log.e(TAG, "DocumentSnapshot successfully deleted!")
                Toast.makeText(this@AnnouncementDetailActivity,"Announcement deleted successfully", Toast.LENGTH_LONG).show()
                dismissProgDialog()
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting document", e)
                Toast.makeText(this@AnnouncementDetailActivity,"Announcement deleting failed try again!", Toast.LENGTH_LONG).show()
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

}