package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
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
import androidx.activity.OnBackPressedCallback
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.FragmentVideosTwoBinding
import com.siffmember.info.ui.adapter.VideoFolderDetailsAdapter
import com.siffmember.info.ui.interfaces.EducationFragmentInteractionInterface
import com.siffmember.info.ui.model.CategoryList
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class VideoTwoFragment : BaseFragment(), VideoFolderDetailsAdapter.VideoDetailsListener{

    companion object {
        private const val TAG = "VideoTwoFragment"
    }

    private lateinit var binding: FragmentVideosTwoBinding
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
        binding = FragmentVideosTwoBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listener!!.onFragmentInteraction(EducationFragments.EDUCATION_DETAILS_TWO_FRAGMENT, TAG)
        db = Firebase.firestore
        setupAdapter()
        fetchAllLevel3Folders()
        binding.apply {
            try {
                val folderOne = EducationDetails.getVideoFolderOne()
                videoTwoTitle.text = folderOne.name ?: ""
                //videoTwoTitle.text = "${EducationDetails.getVideoFolderOne().name} >"
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
                it.name!!.contains(query, ignoreCase = true)
            }
        }
        recyclerViewAdapter!!.updateList(filteredVideos)
    }

    private fun setupAdapter(){
        recyclerViewAdapter = VideoFolderDetailsAdapter(requireActivity(), videoDetailsList, this, sharedPref.getBoolean(
            AppConstants.IS_ADMIN, false))
        binding.videoList.layoutManager = LinearLayoutManager(requireActivity())
        binding.videoList.adapter = recyclerViewAdapter
    }

    private fun fetchAllLevel3Folders() {
        try{
            showProgDialog()
            videoDetailsList.clear()
            val tableName = AppConstants.TABLE_EDUCATION_VIDEO_CONTENT
            db.collection(tableName).document(EducationDetails.getVideoFolderOne().levelId).collection(AppConstants.TABLE_LEVEL_TWO)
                .get()
                .addOnSuccessListener { level1Folders ->
                    if (level1Folders.isEmpty) {
                        handleEmptyVideoList() // No Level 1 folders
                        return@addOnSuccessListener
                    }
                    for (level1Doc in level1Folders) {
                        val level1FolderId = level1Doc.id

                        Log.e(TAG, "fetching Level 1 folders: $level1FolderId")
                        Log.e(TAG, "fetching Level 1 folders: ${level1Doc.getString("name")}")
                        videoDetailsList.add(CategoryList(level1Doc.getString("name")!!, level1FolderId ,level1Doc.getString("createdBy")!!))
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
        EducationDetails.setVideoFolderTwo(videoDetails)
        requireView().findNavController().navigate(R.id.action_twoFragment_to_threeFragment)
    }

    override fun onDelete(videoDetails: CategoryList) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this folder?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLevel2VideoItem(videoDetails)
            }
            .setNegativeButton("Cancel", null)
            .show()
        true
    }

    private fun deleteLevel2VideoItem(video: CategoryList) {
        showProgDialog()

        val level1Id = EducationDetails.getVideoFolderOne().levelId
        val tableName = AppConstants.TABLE_EDUCATION_VIDEO_CONTENT

        val level2Ref = db.collection(tableName)
            .document(level1Id)
            .collection(AppConstants.TABLE_LEVEL_TWO)
            .document(video.levelId)

        val level3Ref = level2Ref.collection(AppConstants.TABLE_LEVEL_THREE)

        // Step 1: Delete all Level 3 documents
        level3Ref.get()
            .addOnSuccessListener { level3Docs ->
                val deleteTasks = mutableListOf<Task<Void>>()

                for (doc in level3Docs) {
                    deleteTasks.add(level3Ref.document(doc.id).delete())
                }

                // Step 2: Wait for all Level 3 deletions to complete
                Tasks.whenAllComplete(deleteTasks)
                    .addOnSuccessListener {
                        // Step 3: Delete Level 2 document
                        level2Ref.delete()
                            .addOnSuccessListener {
                                Toast.makeText(context, "Deleted ${video.name}", Toast.LENGTH_SHORT).show()
                                fetchAllLevel3Folders()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Failed to delete Level 2 document: ", e)
                                Toast.makeText(context, "Failed to delete Level 2", Toast.LENGTH_SHORT).show()
                                dismissProgDialog()
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to delete Level 3 folders: ", e)
                        Toast.makeText(context, "Failed to delete Level 3 documents", Toast.LENGTH_SHORT).show()
                        dismissProgDialog()
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to fetch Level 3 documents: ", e)
                Toast.makeText(context, "Failed to fetch subfolders", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
    }
}