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
import com.siffmember.info.databinding.FragmentPalylistTwoBinding
import com.siffmember.info.ui.activity.ProfileDetailsActivity
import com.siffmember.info.ui.activity.YoutubePlayerActivity
import com.siffmember.info.ui.adapter.VideoDetailsAdapter
import com.siffmember.info.ui.interfaces.AccountInterface
import com.siffmember.info.ui.interfaces.FragmentAccountInteractionInterface
import com.siffmember.info.ui.model.PlayLists
import com.siffmember.info.ui.model.SharedPlaylist
import com.siffmember.info.ui.model.VideoModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class PlayListTwoFragment : BaseFragment(), VideoDetailsAdapter.VideoDetailsListener, SharedPlaylistBottomSheetFragment.BottomSheetListener, AccountInterface{

    companion object {
        private const val TAG = "PlayListTwoFragment"
    }

    private lateinit var binding: FragmentPalylistTwoBinding
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
        binding = FragmentPalylistTwoBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(FragmentsAccounts.PLAYLIST_TWO_FRAGMENT,EducationDetails.getPlayList().name)
        db = Firebase.firestore
        setupAdapter()
        fetchAllPlaylist()
        binding.apply {

           // titlePlaylist.text = EducationDetails.getPlayList().name

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

    fun onDelete(playLists: PlayLists) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this playlist?")
            .setPositiveButton("Delete") { _, _ ->
                deletePlaylist(playLists)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun deletePlaylist(playLists: PlayLists) {
        showProgDialog()
        val docRef = db.collection(AppConstants.TABLE_USER_PLAYLIST)
            .document(sharedPref.getString(AppConstants.USER_ID, "")!!)
            .collection(AppConstants.TABLE_VIDEO_PLAYLIST)
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
        recyclerViewAdapter = VideoDetailsAdapter(requireActivity(), videoDetailsList, this, sharedPref.getBoolean(
            AppConstants.IS_ADMIN, false))
        binding.videoList.layoutManager = LinearLayoutManager(requireActivity())
        binding.videoList.adapter = recyclerViewAdapter
    }

    private fun fetchAllPlaylist() {
        try{
            showProgDialog()
            videoDetailsList.clear()
            val level1DocRef = db.collection(AppConstants.TABLE_USER_PLAYLIST).document(sharedPref.getString(AppConstants.USER_ID, "")!!)
            val level2DocRef = level1DocRef.collection(AppConstants.TABLE_VIDEO_PLAYLIST)
                .document(EducationDetails.getPlayList().playlistId).collection(AppConstants.TABLE_VIDEO_PLAYLIST_VIDEOS)

            level2DocRef.get()
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
                binding.videoSearchEdit.visibility = View.GONE
                binding.noVideo.visibility = View.VISIBLE
            } else {
                binding.videoList.visibility = View.VISIBLE
                binding.videoSearchEdit.visibility = View.VISIBLE
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
            binding.videoSearchEdit.visibility = View.GONE
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
                deletePlaylistVideo(videoDetails)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deletePlaylistVideo(video: VideoModel) {
        showProgDialog()
        val docRef = db.collection(AppConstants.TABLE_USER_PLAYLIST)
            .document(sharedPref.getString(AppConstants.USER_ID, "")!!)
            .collection(AppConstants.TABLE_VIDEO_PLAYLIST).document(EducationDetails.getPlayList().playlistId)
            .collection(AppConstants.TABLE_VIDEO_PLAYLIST_VIDEOS).document(video.id!!)
        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted ${video.videoTitle}", Toast.LENGTH_SHORT).show()
                // Update adapter if using RecyclerView
                fetchAllPlaylist()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete PDF: ", e)
                Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
    }

    override fun onSearchUser(phoneNumber: String) {
        verifyPhoneIsAvailable(phoneNumber)
    }

    private fun verifyPhoneIsAvailable(number: String){
        try{
            showProgDialog()
            val docRef = db.collection(AppConstants.TABLE_USER_DETAILS).document(number)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.data != null) {
                        Log.d("verifyPhoneIsAvailable", "DocumentSnapshot data: ${document.data}")
                        Log.d("verifyPhoneIsAvailable", "DocumentSnapshot data: ${document.data!!["phone_number"]}")
                        onSharedPlaylist(number)
                    } else {
                        dismissProgDialog()
                        Log.d("verifyPhoneIsAvailable", "No such document")
                        Toast.makeText(requireActivity(),"This user not available. Please enter registered user phone number.", Toast.LENGTH_LONG).show()
                    }

                }
                .addOnFailureListener { exception ->
                    Log.d("verifyPhoneIsAvailable", "get failed with ", exception)
                    Toast.makeText(requireActivity(),"This user not available. Please enter registered user phone number.", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        } catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
            Toast.makeText(requireActivity(),"This user not available. Please enter registered user phone number.", Toast.LENGTH_LONG).show()
        }
    }

    fun onSharedPlaylist(userId: String) {
        try {
            val playlistId = EducationDetails.getPlayList().playlistId

            val playLists = SharedPlaylist(
                playlistId,
                EducationDetails.getPlayList().name,
                sharedPref.getString(AppConstants.USER_ID, "")!!,
                sharedPref.getString(AppConstants.USER_NAME, "")!!
            )

            val docRef = db.collection(AppConstants.TABLE_USER_PLAYLIST)
                .document(userId)
                .collection(AppConstants.TABLE_SHARED_VIDEO_PLAYLIST)
                .document(playlistId)

            // Check if document exists first
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        dismissProgDialog()
                        Toast.makeText(requireActivity(),"This playlist already shared", Toast.LENGTH_LONG).show()
                    } else {
                        docRef.set(playLists)
                            .addOnSuccessListener {
                                dismissProgDialog()
                                Toast.makeText(requireActivity(),"This playlist shared successfully", Toast.LENGTH_LONG).show()
                            }
                            .addOnFailureListener { _ ->
                                Toast.makeText(requireActivity(),"Failed to share playlist try again!", Toast.LENGTH_LONG).show()
                                dismissProgDialog()
                            }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error checking playlist existence", e)
                    Toast.makeText(requireActivity(),"Failed to share playlist try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        } catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
            Toast.makeText(requireActivity(),"Failed to share playlist try again!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDeleteClicked() {
        onDelete(EducationDetails.getPlayList())
    }

    override fun onCreateClicked() {
        //
    }

    override fun onShareClicked() {
        if (!videoDetailsList.isEmpty()) {
            val bottomSheetFragment = SharedPlaylistBottomSheetFragment()
            bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
        }
    }
}