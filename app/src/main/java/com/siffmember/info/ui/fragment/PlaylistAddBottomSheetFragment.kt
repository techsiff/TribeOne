package com.siffmember.info.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.PlaylistAddBottomSheetLayoutBinding
import com.siffmember.info.ui.adapter.AddVideoPlaylistAdapter
import com.siffmember.info.ui.model.PlayLists
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class PlaylistAddBottomSheetFragment : BottomSheetDialogFragment(), AddVideoPlaylistAdapter.VideoDetailsListener {

    companion object {
        var TAG = "PlaylistAddBottomSheetFragment"
    }

    private lateinit var binding: PlaylistAddBottomSheetLayoutBinding
    private var listener: BottomSheetListener? = null
    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: AddVideoPlaylistAdapter? = null
    private var playLists = mutableListOf<PlayLists>()
    private var progressDialog: ProgressDialog? = null
    private var userId = ""
    private var selectedUsersList = mutableListOf<PlayLists>()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the parent activity implements the interface
        if (context is BottomSheetListener) {
            listener = context as BottomSheetListener
        } else {
            Log.e(TAG, "Parent activity must implement BottomSheetListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = PlaylistAddBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root
        userId = arguments?.getString("userId")!!
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore
        progressDialog = ProgressDialog(requireActivity())
        setupAdapter()
        fetchAllPlaylists()
        binding.apply {
            btnAddPlaylist.setOnClickListener {
                try {
                    if(selectedUsersList.isEmpty()){
                        Toast.makeText(requireActivity(),"Select one playlist", Toast.LENGTH_LONG).show()
                    } else {
                        addVideoToPlaylist()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
            addNewPlaylist.setOnClickListener {
                try {
                    listener!!.onNewPlaylist()
                    dismiss()
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }
        }
    }
    private fun setupAdapter(){
        try{
            recyclerViewAdapter = AddVideoPlaylistAdapter(playLists, this)
            binding.playlistAddRv.layoutManager = LinearLayoutManager(requireActivity())
            binding.playlistAddRv.adapter = recyclerViewAdapter
        }catch (e: Exception){
            e.printStackTrace()
        }

    }

    private fun fetchAllPlaylists() {
        try{
            showProgDialog()
            playLists.clear()
            val level1DocRef = db.collection(AppConstants.TABLE_USER_PLAYLIST).document(userId)
            val level2DocRef = level1DocRef.collection(AppConstants.TABLE_VIDEO_PLAYLIST)
            level2DocRef.get()
                .addOnSuccessListener { level3Folders ->
                    for (level3Doc in level3Folders) {
                        val name = level3Doc.getString("name") ?: ""
                        val createAt = level3Doc.getString("createdAt") ?: ""

                        val videos = PlayLists(
                            level3Doc.id,
                            name,
                            createAt
                        )
                        playLists.add(videos)
                    }
                    setupAdapter()
                    dismissProgDialog()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching Level 3 folders: $exception")
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    /**
     * Showing progress dialog
     */
    fun showProgDialog() {
        try {
            progressDialog!!.setMode(ProgressDialog.MODE_INDETERMINATE)
            progressDialog!!.show()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Dismiss progress dialog
     */
    fun dismissProgDialog() {
        try{
            if(progressDialog != null){
                progressDialog!!.dismiss()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }


    interface BottomSheetListener {
        fun onNewPlaylist()
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onPlaylistSelectListener(playLists: PlayLists, isSelected: Boolean) {
        if (isSelected) {
            selectedUsersList.add(playLists)
        } else {
            if (selectedUsersList.contains(playLists)) {
                selectedUsersList.remove(playLists)
            }
        }
    }

    private fun addVideoToPlaylist() {
        try {
            for(playList in selectedUsersList){
                val videoDetails = EducationDetails.getVideoDetails()
                showProgDialog()
                val docRef = db.collection(AppConstants.TABLE_USER_PLAYLIST)
                    .document(userId).collection(AppConstants.TABLE_VIDEO_PLAYLIST).document(playList.playlistId)
                    .collection(AppConstants.TABLE_VIDEO_PLAYLIST_VIDEOS).document(videoDetails.id!!)
                docRef.set(videoDetails)
                    .addOnSuccessListener {
                        Log.d(TAG, "DocumentSnapshot successfully written!")
                        dismissProgDialog()
                        Toast.makeText(requireActivity(),"Video added to playlist successfully", Toast.LENGTH_LONG).show()
                        dismiss()
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error writing document", e)
                        Toast.makeText(requireActivity(),"Failed to add video to playlist try again!", Toast.LENGTH_LONG).show()
                        dismissProgDialog()
                        dismiss()
                    }
            }

        } catch (e: Exception){
            e.printStackTrace()
        }
    }
}
