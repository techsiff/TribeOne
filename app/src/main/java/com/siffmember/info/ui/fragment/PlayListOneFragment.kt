package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.FragmentPalylistOneBinding
import com.siffmember.info.ui.activity.ProfileDetailsActivity
import com.siffmember.info.ui.adapter.PlaylistsAdapter
import com.siffmember.info.ui.interfaces.AccountInterface
import com.siffmember.info.ui.interfaces.FragmentAccountInteractionInterface
import com.siffmember.info.ui.model.PlayLists
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class PlayListOneFragment : BaseFragment(), PlaylistsAdapter.VideoDetailsListener, PlaylistCreateBottomSheetFragment.BottomSheetListener, AccountInterface{

    companion object {
        private const val TAG = "PlayListOneFragment"
    }

    private lateinit var binding: FragmentPalylistOneBinding
    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: PlaylistsAdapter? = null
    private var playLists = mutableListOf<PlayLists>()

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
        binding = FragmentPalylistOneBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(FragmentsAccounts.PLAYLIST_ONE_FRAGMENT,"Playlists")
        db = Firebase.firestore
        setupAdapter()
        fetchAllPlaylists()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigateUp()
                }
            })
        (requireActivity() as ProfileDetailsActivity).setAccountInterface(this)
    }

    private fun setupAdapter(){
        playLists.sortByDescending { it.createdAt }
        recyclerViewAdapter = PlaylistsAdapter(requireActivity(), playLists, this)
        binding.playlistRv.layoutManager = LinearLayoutManager(requireActivity())
        binding.playlistRv.adapter = recyclerViewAdapter
    }

    private fun fetchAllPlaylists() {
        try{
            showProgDialog()
            playLists.clear()
            val level1DocRef = db.collection(AppConstants.TABLE_USER_PLAYLIST).document(sharedPref.getString(AppConstants.USER_ID, "")!!)
            val level2DocRef = level1DocRef.collection(AppConstants.TABLE_VIDEO_PLAYLIST)
            level2DocRef.get()
                .addOnSuccessListener { level3Folders ->
                    if (level3Folders.isEmpty) {
                        handleEmptyVideoList() // No Level 1 folders
                        return@addOnSuccessListener
                    }
                    for (level3Doc in level3Folders) {
                       // val playListsId = level3Doc.getString("playListsId") ?: ""
                        val name = level3Doc.getString("name") ?: ""
                        val createAt = level3Doc.getString("createdAt") ?: ""

                        val videos = PlayLists(
                            level3Doc.id,
                            name,
                            createAt
                        )
                        playLists.add(videos)
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
            if (playLists.isEmpty()) {
                binding.playlistRv.visibility = View.GONE
                binding.noPlaylist.visibility = View.VISIBLE
            } else {
                binding.playlistRv.visibility = View.VISIBLE
                binding.noPlaylist.visibility = View.GONE
                setupAdapter()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        dismissProgDialog()
    }

    private fun handleEmptyVideoList() {
        try {
            binding.playlistRv.visibility = View.GONE
            binding.noPlaylist.visibility = View.VISIBLE
            dismissProgDialog()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onClickNext(playLists: PlayLists) {
        EducationDetails.setPlayList(playLists)
        requireView().findNavController().navigate(R.id.action_account_twoFragment_to_threeFragment)
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
                    fetchAllPlaylists()
                    Toast.makeText(requireActivity(),"Playlist created successfully", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(requireActivity(),"Failed to create playlist try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        } catch (e: Exception){
            e.printStackTrace()
        }
    }

    override fun onDeleteClicked() {
        //
    }

    override fun onCreateClicked() {
        val bottomSheetFragment = PlaylistCreateBottomSheetFragment()
        bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
    }

    override fun onShareClicked() {
        //
    }
}