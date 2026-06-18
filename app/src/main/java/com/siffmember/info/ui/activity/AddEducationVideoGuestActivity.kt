package com.siffmember.info.ui.activity

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.data.remote.api.RetrofitInstanceYoutube
import com.siffmember.info.databinding.ActivityAddEducationVideoGuestBinding
import com.siffmember.info.ui.model.VideoModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails
import com.siffmember.info.utils.OpenPoints
import kotlinx.coroutines.launch

class AddEducationVideoGuestActivity : BaseActivity() {

    companion object {
        var TAG = "AddEducationVideoGuestActivity"
    }

    private lateinit var binding: ActivityAddEducationVideoGuestBinding
    private lateinit var db: FirebaseFirestore

    private var videoId = ""
    private var videoTitle = ""
    private var videoBy = ""
    private var videoDescription = ""
    private var videoThumbnail = ""
    private var videoDuration = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEducationVideoGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->

            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // Push entire layout up
            view.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        db = Firebase.firestore

        binding.apply {
            btnAdd.setOnClickListener {
                if(validate()) {
                    showProgDialog()
                    if(videoDuration.isEmpty()){
                        getVideoDuration(videoId)
                    }
                }
            }
        }
    }

    private fun validate(): Boolean{

        if(binding.videoIdEdit.text.toString().trim().isNotEmpty()){
            videoId = binding.videoIdEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationVideoGuestActivity,"Please enter video id", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.videoTitleEdit.text.toString().trim().isNotEmpty()){
            videoTitle = binding.videoTitleEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationVideoGuestActivity,"Please enter video title", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.videoByEdit.text.toString().trim().isNotEmpty()){
            videoBy = binding.videoByEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationVideoGuestActivity,"Please enter video by", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.videoDescEdit.text.toString().trim().isNotEmpty()){
            videoDescription = binding.videoDescEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationVideoGuestActivity,"Please enter video description", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun addVideo(){
        try{
            // Level 1 Folder: Free Trainings
            val docRef = db.collection(AppConstants.TABLE_EDUCATION_VIDEO_CONTENT_GUEST).document()
            val videoData = VideoModel(docRef.id, videoDescription, videoBy, videoDuration, videoId, videoThumbnail, videoTitle)
            docRef
                .set(videoData)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    binding.videoIdEdit.setText("")
                    binding.videoTitleEdit.setText("")
                    binding.videoByEdit.setText("")
                    binding.videoDescEdit.setText("")
                    videoId = ""
                    videoTitle = ""
                    videoBy = ""
                    videoDescription = ""
                    videoThumbnail = ""
                    videoDuration = ""
                    dismissProgDialog()
                    EducationDetails.setVideoContentAdd(true)
                    Toast.makeText(this@AddEducationVideoGuestActivity,"Added successfully", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@AddEducationVideoGuestActivity,"Failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    private fun getVideoDuration(videoId: String){
        try{
            val apiKey = OpenPoints.getApiKey()
            // Load duration
            lifecycleScope.launch {
                val duration = getVideoDuration(videoId, apiKey)
                if(duration != null){
                    Log.e(TAG, "duration: $duration")
                    videoDuration = duration
                   // videoThumbnail = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                    videoThumbnail = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                    addVideo()
                } else {
                    Log.e(TAG, "video not available")
                    Toast.makeText(this@AddEducationVideoGuestActivity,"Video not available. Please enter correct video id.", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
            }
        }catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }
    private suspend fun getVideoDuration(videoId: String, apiKey: String): String? {
        return try {
            val response = RetrofitInstanceYoutube.youtubeService.getVideoDetails("contentDetails", videoId, apiKey)
            if (response.items.isNotEmpty()) {
                // Parse the ISO 8601 duration format to something user-friendly (e.g., "PT1H2M10S" to "1:02:10")
                parseDuration(response.items[0].contentDetails.duration)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseDuration(isoDuration: String): String {
        val regex = Regex("PT(?:(\\d+)H)?(?:(\\d+)M)?(?:(\\d+)S)?")
        val matchResult = regex.matchEntire(isoDuration)
        val (hours, minutes, seconds) = matchResult?.destructured ?: return "00:00:00"

        val hoursPadded = hours.padStart(2, '0')
        val minutesPadded = (minutes.ifEmpty { "0" }).padStart(2, '0')
        val secondsPadded = (seconds.ifEmpty { "0" }).padStart(2, '0')

        return "$hoursPadded:$minutesPadded:$secondsPadded"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}