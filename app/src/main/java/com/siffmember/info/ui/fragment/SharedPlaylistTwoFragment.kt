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
import com.siffmember.info.databinding.FragmentSharedPalylistTwoBinding
import com.siffmember.info.ui.activity.ProfileDetailsActivity
import com.siffmember.info.ui.activity.YoutubePlayerActivity
import com.siffmember.info.ui.adapter.VideoDetailsAdapter
import com.siffmember.info.ui.interfaces.AccountInterface
import com.siffmember.info.ui.interfaces.FragmentAccountInteractionInterface
import com.siffmember.info.ui.model.SharedPlaylist
import com.siffmember.info.ui.model.VideoModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class SharedPlaylistTwoFragment : BaseFragment(), VideoDetailsAdapter.VideoDetailsListener, AccountInterface{

    companion object {
        private const val TAG = "SharedPlaylistTwoFragment"
    }

    private lateinit var binding: FragmentSharedPalylistTwoBinding
    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: VideoDetailsAdapter? = null
    private var videoDetailsList = mutableListOf<VideoModel>()
    private var listener: FragmentAccountInteractionInterface? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = if (context is FragmentAccountInteractionInterface) {
            context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentSharedPalylistTwoBinding.inflate(inflater, container, false)
        val root = binding.root
        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(FragmentsAccounts.SHARED_PLAYLIST_TWO_FRAGMENT,EducationDetails.getSharedPlayList().name)
        db = Firebase.firestore
        setupAdapter()
        fetchAllPlaylist()
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
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            })

        (requireActivity() as ProfileDetailsActivity).setAccountInterface(this)
    }

    private fun onDelete(playLists: SharedPlaylist) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this shared playlist?")
            .setPositiveButton("Delete") { _, _ ->
                deletePlaylist(playLists)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun deletePlaylist(playLists: SharedPlaylist) {
        showProgDialog()
        val docRef = db.collection(AppConstants.TABLE_USER_PLAYLIST)
            .document(sharedPref.getString(AppConstants.USER_ID, "")!!)
            .collection(AppConstants.TABLE_SHARED_VIDEO_PLAYLIST)
            .document(playLists.playlistId)
        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted ${playLists.name}", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
                findNavController().navigateUp()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete PDF: ", e)
                Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
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
        recyclerViewAdapter = VideoDetailsAdapter(requireActivity(), videoDetailsList, this, false)
        binding.videoList.layoutManager = LinearLayoutManager(requireActivity())
        binding.videoList.adapter = recyclerViewAdapter
    }

    private fun fetchAllPlaylist() {
        try{
            showProgDialog()
            videoDetailsList.clear()
            val level1DocRef = db.collection(AppConstants.TABLE_USER_PLAYLIST).document(EducationDetails.getSharedPlayList().sharedUserId)
            val level2DocRef = level1DocRef.collection(AppConstants.TABLE_VIDEO_PLAYLIST)
                .document(EducationDetails.getSharedPlayList().playlistId).collection(AppConstants.TABLE_VIDEO_PLAYLIST_VIDEOS)

            level2DocRef.get()
                .addOnSuccessListener { level3Folders ->
                    if (level3Folders.isEmpty) {
                        dismissProgDialog()
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
                        val level1Id = level3Doc.getString("level1Id") ?: ""
                        val level2Id = level3Doc.getString("level2Id") ?: ""

                        val videos = VideoModel(
                            level3Doc.id,
                            description,
                            videoBy,
                            videoDuration,
                            videoId,
                            videoThumbnail,
                            videoTitle,
                            level2Id,
                            level1Id
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
        //
    }

    override fun onDeleteClicked() {
        onDelete(EducationDetails.getSharedPlayList())
    }

    override fun onCreateClicked() {
        //
    }

    override fun onShareClicked() {
        //
    }
}