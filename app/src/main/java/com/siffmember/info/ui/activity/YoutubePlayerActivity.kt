package com.siffmember.info.ui.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView
import com.siffmember.info.databinding.ActivityYoutubePlayerBinding
import com.siffmember.info.ui.fragment.PlaylistAddBottomSheetFragment
import com.siffmember.info.ui.fragment.PlaylistCreateBottomSheetFragment
import com.siffmember.info.ui.model.PlayLists
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails
import com.siffmember.info.utils.OpenPoints


class YoutubePlayerActivity : BaseActivity(), PlaylistAddBottomSheetFragment.BottomSheetListener, PlaylistCreateBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "YoutubePlayerActivity"
    }
    private lateinit var binding: ActivityYoutubePlayerBinding
    private lateinit var db: FirebaseFirestore
    private var youTubePlayer: YouTubePlayer? = null
    private var isFullscreen = false

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
        )

        binding = ActivityYoutubePlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeaderVideo) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        db = Firebase.firestore
        try{
            val videoDetails = EducationDetails.getVideoDetails()
            val youtubePlayerView: YouTubePlayerView = binding.youtubeWebView
            val fullscreenViewContainer = binding.fullScreenViewContainer

            binding.apply {
                videoTitleTxt.text = videoDetails.videoTitle
                videoDescTxt.text = videoDetails.description

                if(OpenPoints.getIsGuestUser()){
                    addPlaylist.visibility = View.GONE
                } else {
                    addPlaylist.visibility = View.VISIBLE
                }
                addPlaylist.setOnClickListener {
                    addVideoToPlaylist()
                }
            }
            val videoId = videoDetails.videoId
            Log.e(TAG, "ID:: $videoId")

            val iFramePlayerOptions = IFramePlayerOptions.Builder(applicationContext)
                .controls(1)
                .fullscreen(1) // enable full screen button
                .build()

            youtubePlayerView.enableAutomaticInitialization = false

            youtubePlayerView.addFullscreenListener(object : FullscreenListener {
                override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                    isFullscreen = true
                    // the video will continue playing in fullscreenView
                    youtubePlayerView.visibility = View.GONE
                    fullscreenViewContainer.visibility = View.VISIBLE
                    fullscreenViewContainer.addView(fullscreenView)

                    // optionally request landscape orientation
                    // requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }

                override fun onExitFullscreen() {
                    isFullscreen = false

                    // the video will continue playing in the player
                    youtubePlayerView.visibility = View.VISIBLE
                    fullscreenViewContainer.visibility = View.GONE
                    fullscreenViewContainer.removeAllViews()
                }
            })

            youtubePlayerView.initialize(object : AbstractYouTubePlayerListener() {
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    this@YoutubePlayerActivity.youTubePlayer = youTubePlayer
                    youTubePlayer.cueVideo( videoId!!, 0f)
                }
            }, iFramePlayerOptions)

            lifecycle.addObserver(youtubePlayerView)

        } catch (e: Exception){
            e.printStackTrace()
        }
    }
    private fun addVideoToPlaylist(){
        val bottomSheetFragment = PlaylistAddBottomSheetFragment()
        val bundle = Bundle().apply {
            putString("userId", sharedPref.getString(AppConstants.USER_ID, ""))
        }
        bottomSheetFragment.arguments = bundle
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    override fun onDestroy() {
        super.onDestroy()
        binding.youtubeWebView.release()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onNewPlaylist() {
        val bottomSheetFragment = PlaylistCreateBottomSheetFragment()
        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
    }

    override fun onCreatePlaylist(title: String) {
        try {
            showProgDialog()
            val currentTimestamp = System.currentTimeMillis()
            val docRef = db.collection(AppConstants.TABLE_USER_PLAYLIST)
                .document(sharedPref.getString(AppConstants.USER_ID, "")!!)
            docRef.set(mapOf("name" to "VideoPlaylist", "createdBy" to sharedPref.getString(AppConstants.USER_NAME, "")), SetOptions.merge())
            val docRef1= docRef.collection(AppConstants.TABLE_VIDEO_PLAYLIST).document()
            val playLists = PlayLists(docRef1.id, title, currentTimestamp.toString())
            docRef1.set(playLists)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    dismissProgDialog()
                    Toast.makeText(this@YoutubePlayerActivity,"Playlist created successfully", Toast.LENGTH_LONG).show()
                    addVideoToPlaylist()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@YoutubePlayerActivity,"Failed to create playlist try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }
}