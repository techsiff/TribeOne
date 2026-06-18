package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
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
import com.rajat.pdfviewer.PdfViewerActivity
import com.rajat.pdfviewer.util.saveTo
import com.siffmember.info.databinding.FragmentPdfThreeBinding
import com.siffmember.info.ui.adapter.PDFDetailsAdapter
import com.siffmember.info.ui.interfaces.EducationFragmentInteractionInterface
import com.siffmember.info.ui.model.PDFModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class PDFThreeFragment : BaseFragment(), PDFDetailsAdapter.PDFDetailsListener {

    companion object {
        private const val TAG = "PDFThreeFragment"
    }

    private var _binding: FragmentPdfThreeBinding? = null
    private val binding get() = _binding!!

    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: PDFDetailsAdapter? = null
    private var pdfDetailsList: ArrayList<PDFModel> = ArrayList()
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
        _binding = FragmentPdfThreeBinding.inflate(inflater, container, false)
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
            try{
                val folderOne = EducationDetails.getVideoFolderOne()
                val folderTwo = EducationDetails.getVideoFolderTwo()
                pdfThreeTitle.text = "${folderOne.name ?: ""} > ${folderTwo.name ?: ""}"
                //pdfThreeTitle.text = "${EducationDetails.getVideoFolderOne().name} > ${EducationDetails.getVideoFolderTwo().name}"
            }catch (e: Exception){
                e.printStackTrace()
            }
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
                it.pdfTitle!!.contains(query, ignoreCase = true)
            }
        }
        recyclerViewAdapter!!.updateList(filteredVideos)
    }

    private fun setupAdapter(){
        recyclerViewAdapter = PDFDetailsAdapter(pdfDetailsList, this, sharedPref.getBoolean(
            AppConstants.IS_ADMIN, false))
        binding.pdfList.layoutManager = LinearLayoutManager(requireActivity())
        binding.pdfList.adapter = recyclerViewAdapter
    }


    private fun fetchAllLevel3Folders() {
        try{
            showProgDialog()
            pdfDetailsList.clear()
            val tableName = AppConstants.TABLE_EDUCATION_PDF_CONTENT
            val level1DocRef = db.collection(tableName).document(EducationDetails.getVideoFolderOne().levelId)
            val level2DocRef = level1DocRef.collection(AppConstants.TABLE_LEVEL_TWO).document(EducationDetails.getVideoFolderTwo().levelId)
            val level3DocRef = level2DocRef.collection(AppConstants.TABLE_LEVEL_THREE)

            level3DocRef.get()
                .addOnSuccessListener { level1Folders ->
                    if (level1Folders.isEmpty) {
                        handleEmptyVideoList() // No Level 1 folders
                        return@addOnSuccessListener
                    }

                    for (level3Doc in level1Folders) {
                        val level3FolderId = level3Doc.id
                        val pdfURL = level3Doc.getString("pdfURL") ?: ""
                        val pdfTitle = level3Doc.getString("pdfTitle") ?: ""
                        val pdfBy = level3Doc.getString("pdfBy") ?: ""
                        val description = level3Doc.getString("description") ?: ""

                        val pdfs = PDFModel(
                            level3FolderId,
                            description,
                            pdfBy,
                            pdfURL,
                            pdfTitle,
                            EducationDetails.getVideoFolderTwo().levelId,
                            EducationDetails.getVideoFolderOne().levelId
                        )
                        pdfDetailsList.add(pdfs)
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


    // Function to update the UI after data fetch is complete
    private fun updateUIAfterDataFetch() {
        try {
            if (pdfDetailsList.isEmpty()) {
                //Toast.makeText(requireActivity(), "Education pdf not available.", Toast.LENGTH_LONG).show()
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

    // Function to handle empty video list case
    private fun handleEmptyVideoList() {
        //Toast.makeText(requireActivity(), "Education pdf not available.", Toast.LENGTH_LONG).show()
        binding.pdfList.visibility = View.GONE
        binding.noPdf.visibility = View.VISIBLE
        dismissProgDialog()
    }

    override fun onClickNext(pdfDetails: PDFModel?) {
        if (Patterns.WEB_URL.matcher(pdfDetails!!.pdfURL!!).matches() && pdfDetails.pdfURL.endsWith(".pdf", ignoreCase = true)) {
            startActivity(
                PdfViewerActivity.launchPdfFromUrl(
                    context = requireActivity(),
                    pdfUrl = pdfDetails.pdfURL,
                    pdfTitle = pdfDetails.pdfTitle,
                    saveTo = saveTo.ASK_EVERYTIME,
                    enableDownload = false
                )
            )
        } else {
            Toast.makeText(requireActivity(),"The pdf URL not valid", Toast.LENGTH_LONG).show()
        }
    }

    override fun onDelete(pdfDetails: PDFModel?) {
        AlertDialog.Builder(context)
            .setTitle("Delete")
            .setMessage("Are you sure you want to delete this PDF file?")
            .setPositiveButton("Delete") { _, _ ->
                deleteLevel3PDFItem(pdfDetails!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
        true
    }
    @SuppressLint("NotifyDataSetChanged")
    private fun deleteLevel3PDFItem(pdf: PDFModel) {
        showProgDialog()
        val tableName = AppConstants.TABLE_EDUCATION_PDF_CONTENT

        val docRef = db.collection(tableName)
            .document(pdf.level1Id)
            .collection(AppConstants.TABLE_LEVEL_TWO)
            .document(pdf.level2Id)
            .collection(AppConstants.TABLE_LEVEL_THREE)
            .document(pdf.id!!)
        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted ${pdf.pdfTitle}", Toast.LENGTH_SHORT).show()
                // Update adapter if using RecyclerView
               fetchAllLevel3Folders()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to delete PDF: ", e)
                Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}