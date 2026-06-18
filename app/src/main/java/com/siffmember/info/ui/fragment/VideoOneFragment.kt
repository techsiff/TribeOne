package com.siffmember.info.ui.fragment

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.FragmentVideosOneBinding
import com.siffmember.info.ui.adapter.VideoFolderDetailsAdapter
import com.siffmember.info.ui.interfaces.EducationFragmentInteractionInterface
import com.siffmember.info.ui.model.CategoryList
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class VideoOneFragment : BaseFragment(), VideoFolderDetailsAdapter.VideoDetailsListener{

    companion object {
        private const val TAG = "VideoOneFragment"
    }

    private lateinit var binding: FragmentVideosOneBinding
    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: VideoFolderDetailsAdapter? = null
    private var videoDetailsList = mutableListOf<CategoryList>()
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
        binding = FragmentVideosOneBinding.inflate(inflater, container, false)
        val root = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(EducationFragments.EDUCATION_DETAILS_ONE_FRAGMENT, TAG)
        db = Firebase.firestore
        setupAdapter()
        fetchAllLevel3Folders()
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
            fetchAllLevel3Folders()
        }
    }

    private fun filterList(query: String) {
        val filteredVideos = if (query.isEmpty()) {
            videoDetailsList  // Reset to full list when query is empty
        } else {
            videoDetailsList.filter {
                it.name!!.contains(query, ignoreCase = true)
            }
        }
        recyclerViewAdapter!!.updateList(filteredVideos)
    }

    private fun setupAdapter(){
        recyclerViewAdapter = VideoFolderDetailsAdapter(requireActivity(), videoDetailsList, this, sharedPref.getBoolean(
            AppConstants.IS_ADMIN, false))
        // Sets up the RecyclerView.
        binding.videoList.layoutManager = LinearLayoutManager(requireActivity())
        binding.videoList.adapter = recyclerViewAdapter
    }

    private fun fetchAllLevel3Folders() {
        try{
            showProgDialog()
            videoDetailsList.clear()
            val tableName = AppConstants.TABLE_EDUCATION_VIDEO_CONTENT
            db.collection(tableName).get()
                .addOnSuccessListener { level1Folders ->
                    if (level1Folders.isEmpty) {
                        handleEmptyVideoList() // No Level 1 folders
                        return@addOnSuccessListener
                    }
                    for (level1Doc in level1Folders) {
                        val level1FolderId = level1Doc.id
                        val access = level1Doc.get("allowedRoles") as List<String>
                        val name = level1Doc.getString("name")!!
                        val createdBy =level1Doc.getString("createdBy")!!
                        if (access.contains(sharedPref.getString(AppConstants.USER_CATEGORY, null))) {
                            videoDetailsList.add(CategoryList(name,level1FolderId, createdBy,access))
                        }
                        updateUIAfterDataFetch()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching Level 1 folders: $exception")
                    dismissProgDialog()
                }

        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun updateUIAfterDataFetch() {
        try{
            dismissProgDialog()
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

    override fun onClickNext(videoDetails: CategoryList) {
        EducationDetails.setVideoFolderOne(videoDetails)
        requireView().findNavController().navigate(R.id.action_oneFragment_to_twoFragment)
    }

    override fun onDelete(videoDetails: CategoryList) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this folder?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLevel1VideoItem(videoDetails)
            }
            .setNegativeButton("Cancel", null)
            .show()
        true
    }

    private fun deleteLevel1VideoItem(video: CategoryList) {
        showProgDialog()
        val level1Ref = db.collection(AppConstants.TABLE_EDUCATION_VIDEO_CONTENT).document(video.levelId)

        level1Ref.collection(AppConstants.TABLE_LEVEL_TWO).get()
            .addOnSuccessListener { level2Docs ->
                val allDeleteTasks = mutableListOf<Task<Void>>()

                if (level2Docs.isEmpty) {
                    // No Level 2 folders, delete level 1 directly
                    level1Ref.delete()
                        .addOnSuccessListener {
                            Toast.makeText(context, "Deleted ${video.name}", Toast.LENGTH_SHORT).show()
                            fetchAllLevel3Folders()
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to Deleted ${video.name}", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                    return@addOnSuccessListener
                }

                var remaining = level2Docs.size()

                for (level2Doc in level2Docs) {
                    val level2Ref = level1Ref.collection(AppConstants.TABLE_LEVEL_TWO).document(level2Doc.id)

                    level2Ref.collection(AppConstants.TABLE_LEVEL_THREE).get()
                        .addOnSuccessListener { level3Docs ->
                            for (level3Doc in level3Docs) {
                                val level3Ref = level2Ref.collection(AppConstants.TABLE_LEVEL_THREE).document(level3Doc.id)
                                allDeleteTasks.add(level3Ref.delete())
                            }

                            // Delete Level 2 doc after deleting its Level 3 docs
                            allDeleteTasks.add(level2Ref.delete())

                            remaining--
                            if (remaining == 0) {
                                // All level2 folders processed
                                Tasks.whenAllComplete(allDeleteTasks)
                                    .addOnSuccessListener {
                                        level1Ref.delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "Deleted ${video.name}", Toast.LENGTH_SHORT).show()
                                                fetchAllLevel3Folders()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(context, "Failed Deleted ${video.name}", Toast.LENGTH_SHORT).show()
                                                dismissProgDialog()
                                            }
                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(context, "Failed to delete nested folders", Toast.LENGTH_SHORT).show()
                                        dismissProgDialog()
                                    }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to fetch Level 3 folders", Toast.LENGTH_SHORT).show()
                            dismissProgDialog()
                        }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to fetch Level 2 folders", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
    }
}