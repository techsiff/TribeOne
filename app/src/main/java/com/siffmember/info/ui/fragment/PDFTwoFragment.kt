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
import com.siffmember.info.databinding.FragmentPdfTwoBinding
import com.siffmember.info.ui.adapter.PDFFolderDetailsAdapter
import com.siffmember.info.ui.interfaces.EducationFragmentInteractionInterface
import com.siffmember.info.ui.model.CategoryList
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class PDFTwoFragment : BaseFragment(), PDFFolderDetailsAdapter.PDFDetailsListener {

    companion object {
        private const val TAG = "PDFTwoFragment"
    }

    private var _binding: FragmentPdfTwoBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: PDFFolderDetailsAdapter? = null
    private var pdfDetailsList: ArrayList<CategoryList> = ArrayList()
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
        _binding = FragmentPdfTwoBinding.inflate(inflater, container, false)
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
            try{
                val folderOne = EducationDetails.getVideoFolderOne()
                pdfTwoTitle.text = folderOne.name ?: ""
            }catch (e: Exception){
                e.printStackTrace()
            }
            //pdfTwoTitle.text = "${EducationDetails.getVideoFolderOne().name} >"
            pdfSearchEdit.addTextChangedListener(object : TextWatcher {
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
        if(EducationDetails.getPDFContentAdd()){
            EducationDetails.setPDFContentAdd(false)
            fetchAllLevel3Folders()
        }
    }
    private fun filterList(query: String) {
        val filteredVideos = if (query.isEmpty()) {
            pdfDetailsList  // Reset to full list when query is empty
        } else {
            pdfDetailsList.filter {
                it.name!!.contains(query, ignoreCase = true)
            }
        }
        recyclerViewAdapter!!.updateList(filteredVideos)
    }

    private fun setupAdapter(){
        recyclerViewAdapter = PDFFolderDetailsAdapter(pdfDetailsList, this, sharedPref.getBoolean(
            AppConstants.IS_ADMIN, false))
        binding.pdfList.layoutManager = LinearLayoutManager(requireActivity())
        binding.pdfList.adapter = recyclerViewAdapter
    }

    private fun fetchAllLevel3Folders() {
        try{
            showProgDialog()
            pdfDetailsList.clear()
            val tableName = AppConstants.TABLE_EDUCATION_PDF_CONTENT
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
                        pdfDetailsList.add(CategoryList(level1Doc.getString("name")!!, level1FolderId ,level1Doc.getString("createdBy")!!))
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
        try {
            if (pdfDetailsList.isEmpty()) {
                binding.pdfList.visibility = View.GONE
                binding.noPdf.visibility = View.VISIBLE
            } else {
                binding.pdfList.visibility = View.VISIBLE
                binding.noPdf.visibility = View.GONE
                setupAdapter()
            }
            dismissProgDialog()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun handleEmptyVideoList() {
        binding.pdfList.visibility = View.GONE
        binding.noPdf.visibility = View.VISIBLE
        dismissProgDialog()
    }

    override fun onClickNext(pdfDetails: CategoryList?) {
        EducationDetails.setVideoFolderTwo(pdfDetails!!)
        requireView().findNavController().navigate(R.id.action_twoFragment_to_threeFragment)
    }

    override fun onDelete(pdfDetails: CategoryList?) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this folder?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLevel2PDFItem(pdfDetails!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
        true
    }

    private fun deleteLevel2PDFItem(video: CategoryList) {
        showProgDialog()

        val level1Id = EducationDetails.getVideoFolderOne().levelId
        val tableName = AppConstants.TABLE_EDUCATION_PDF_CONTENT

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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}