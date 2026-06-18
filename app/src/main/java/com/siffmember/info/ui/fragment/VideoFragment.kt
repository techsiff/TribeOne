package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.FragmentVideosBinding
import com.siffmember.info.ui.activity.YoutubePlayerActivity
import com.siffmember.info.ui.adapter.VideoDetailsAdapter
import com.siffmember.info.ui.model.VideoModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class VideoFragment : BaseFragment(), VideoDetailsAdapter.VideoDetailsListener{

    companion object {
        private const val TAG = "VideoFragment"
    }

    private lateinit var binding: FragmentVideosBinding
    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: VideoDetailsAdapter? = null
    private var videoDetailsList = mutableListOf<VideoModel>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentVideosBinding.inflate(inflater, container, false)
        val root = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore
        setupAdapter()
        fetchAllGuestVideo()
        binding.apply {
            videoSearchEdit.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                    //
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    filterList(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {
                    //
                }
            })
        }
    }

    override fun onResume() {
        super.onResume()
        if(EducationDetails.getVideoContentAdd()){
            EducationDetails.setVideoContentAdd(false)
            fetchAllGuestVideo()
        }
    }

    private fun filterList(query: String) {
        val filteredVideos = if (query.isEmpty()) {
            videoDetailsList  // Reset to full list when query is empty
        } else {
            videoDetailsList.filter {
                it.videoTitle!!.contains(query, ignoreCase = true)
            }
        }
        recyclerViewAdapter!!.updateList(filteredVideos)
    }

    private fun setupAdapter(){
        recyclerViewAdapter = VideoDetailsAdapter(requireActivity(), videoDetailsList, this, sharedPref.getBoolean(
            AppConstants.IS_ADMIN, false))
        binding.videoList.layoutManager = LinearLayoutManager(requireActivity())
        binding.videoList.adapter = recyclerViewAdapter
    }

    private fun fetchAllGuestVideo() {
        try{
            showProgDialog()
            videoDetailsList.clear()
            val docRef = db.collection(AppConstants.TABLE_EDUCATION_VIDEO_CONTENT_GUEST)
            docRef.get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        handleEmptyVideoList() // No Level 1 folders
                        return@addOnSuccessListener
                    }

                    for (level1Doc in documents) {
                        val videoId = level1Doc.getString("videoId") ?: ""
                        val videoTitle = level1Doc.getString("videoTitle") ?: ""
                        val videoThumbnail = level1Doc.getString("videoThumbnail") ?: ""
                        val videoDuration = level1Doc.getString("videoDuration") ?: ""
                        val videoBy = level1Doc.getString("videoBy") ?: ""
                        val description = level1Doc.getString("description") ?: ""

                        val videos = VideoModel(
                            level1Doc.id,
                            description,
                            videoBy,
                            videoDuration,
                            videoId,
                            videoThumbnail,
                            videoTitle,
                        )
                        videoDetailsList.add(videos)
                    }

                    updateUIAfterDataFetch()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching guest videos: $exception")
                    dismissProgDialog()
                }

        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    // Function to update the UI after data fetch is complete
    private fun updateUIAfterDataFetch() {
        try{
            if (videoDetailsList.isEmpty()) {
                binding.videoList.visibility = View.GONE
                binding.noVideo.visibility = View.VISIBLE
            } else {
                binding.videoList.visibility = View.VISIBLE
                binding.noVideo.visibility = View.GONE
                setupAdapter()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        dismissProgDialog()
    }

    // Function to handle empty video list case
    private fun handleEmptyVideoList() {
        try {
            binding.videoList.visibility = View.GONE
            binding.noVideo.visibility = View.VISIBLE
            dismissProgDialog()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onClickNext(videoDetails: VideoModel) {
        EducationDetails.setVideoDetails(videoDetails)
        val nextIntent = Intent(requireActivity(), YoutubePlayerActivity::class.java)
        startActivity(nextIntent)
    }

    override fun onDelete(videoDetails: VideoModel) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this video?")
            .setPositiveButton("Delete") { _, _ ->
                deleteGuestVideoItem(videoDetails)
            }
            .setNegativeButton("Cancel", null)
            .show()
        true
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteGuestVideoItem(video: VideoModel) {
        showProgDialog()
        val docRef =  db.collection(AppConstants.TABLE_EDUCATION_VIDEO_CONTENT_GUEST).document(video.id!!)
        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted ${video.videoTitle}", Toast.LENGTH_SHORT).show()
               fetchAllGuestVideo()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
    }
}