package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.FragmentVideosThreeBinding
import com.siffmember.info.ui.activity.YoutubePlayerActivity
import com.siffmember.info.ui.adapter.VideoDetailsAdapter
import com.siffmember.info.ui.interfaces.EducationFragmentInteractionInterface
import com.siffmember.info.ui.model.VideoModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class VideoThreeFragment : BaseFragment(), VideoDetailsAdapter.VideoDetailsListener{

    companion object {
        private const val TAG = "VideoThreeFragment"
    }

    private lateinit var binding: FragmentVideosThreeBinding
    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: VideoDetailsAdapter? = null
    private var videoDetailsList = mutableListOf<VideoModel>()
    private var listener: EducationFragmentInteractionInterface? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is EducationFragmentInteractionInterface) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentVideosThreeBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(EducationFragments.EDUCATION_DETAILS_THREE_FRAGMENT, TAG)
        db = Firebase.firestore
        setupAdapter()
        fetchAllLevel3Folders()
        binding.apply {
            try {
                val folderOne = EducationDetails.getVideoFolderOne()
                val folderTwo = EducationDetails.getVideoFolderTwo()
                videoThreeTitle.text = "${folderOne.name ?: ""} > ${folderTwo.name ?: ""}"
               // videoThreeTitle.text = "${EducationDetails.getVideoFolderOne().name} > ${EducationDetails.getVideoFolderTwo().name}"
            }catch (e: Exception){
                e.printStackTrace()
            }
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
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            })
    }

    override fun onResume() {
        super.onResume()
        if(EducationDetails.getVideoContentAdd()){
            EducationDetails.setVideoContentAdd(false)
            fetchAllLevel3Folders()
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

    private fun fetchAllLevel3Folders() {
        try{
            showProgDialog()
            videoDetailsList.clear()
            val tableName = AppConstants.TABLE_EDUCATION_VIDEO_CONTENT
            val level1DocRef = db.collection(tableName).document(EducationDetails.getVideoFolderOne().levelId)
            val level2DocRef = level1DocRef.collection(AppConstants.TABLE_LEVEL_TWO).document(EducationDetails.getVideoFolderTwo().levelId)
            val level3DocRef = level2DocRef.collection(AppConstants.TABLE_LEVEL_THREE)

            level3DocRef.get()
                .addOnSuccessListener { level3Folders ->
                    if (level3Folders.isEmpty) {
                        handleEmptyVideoList() // No Level 1 folders
                        return@addOnSuccessListener
                    }
                    for (level3Doc in level3Folders) {
                        val videoId = level3Doc.getString("videoId") ?: ""
                        val videoTitle = level3Doc.getString("videoTitle") ?: ""
                        val videoThumbnail = level3Doc.getString("videoThumbnail") ?: ""
                        val videoDuration = level3Doc.getString("videoDuration") ?: ""
                        val videoBy = level3Doc.getString("videoBy") ?: ""
                        val description = level3Doc.getString("description") ?: ""

                        val videos = VideoModel(
                            level3Doc.id,
                            description,
                            videoBy,
                            videoDuration,
                            videoId,
                            videoThumbnail,
                            videoTitle,
                            EducationDetails.getVideoFolderTwo().levelId,
                            EducationDetails.getVideoFolderOne().levelId
                        )
                        videoDetailsList.add(videos)
                    }
                    updateUIAfterDataFetch()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching Level 3 folders: $exception")
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
            .setMessage("Are you sure you want to delete this video file?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLevel3VideoItem(videoDetails)
            }
            .setNegativeButton("Cancel", null)
            .show()
        true
    }
    private fun deleteLevel3VideoItem(video: VideoModel) {
        showProgDialog()
        val tableName = AppConstants.TABLE_EDUCATION_VIDEO_CONTENT
        val docRef = db.collection(tableName)
            .document(video.level1Id)
            .collection(AppConstants.TABLE_LEVEL_TWO)
            .document(video.level2Id)
            .collection(AppConstants.TABLE_LEVEL_THREE)
            .document(video.id!!)

        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted ${video.videoTitle}", Toast.LENGTH_SHORT).show()
                // Update adapter if using RecyclerView
                fetchAllLevel3Folders()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete PDF: ", e)
                Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
    }
}