package com.siffmember.info.ui.fragment

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.rajat.pdfviewer.PdfViewerActivity
import com.rajat.pdfviewer.util.saveTo
import com.siffmember.info.databinding.FragmentPdfBinding
import com.siffmember.info.ui.adapter.PDFDetailsAdapter
import com.siffmember.info.ui.model.PDFModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class PDFFragment : BaseFragment(), PDFDetailsAdapter.PDFDetailsListener {

    companion object {
        private const val TAG = "PDFFragment"
    }

    private lateinit var binding: FragmentPdfBinding
    private lateinit var db: FirebaseFirestore
    private var recyclerViewAdapter: PDFDetailsAdapter? = null
    private var pdfDetailsList: ArrayList<PDFModel> = ArrayList()
    
    override fun onCreateView(inflater:  LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentPdfBinding.inflate(inflater, container, false)
        val root = binding.root
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore
        setupAdapter()
        fetchAllGuestPDF()
        binding.apply {
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
    }

    override fun onResume() {
        super.onResume()
        if(EducationDetails.getPDFContentAdd()){
            EducationDetails.setPDFContentAdd(false)
            fetchAllGuestPDF()
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

    private fun fetchAllGuestPDF() {
        try{
            showProgDialog()
            pdfDetailsList.clear()
            val docRef = db.collection(AppConstants.TABLE_EDUCATION_PDF_CONTENT_GUEST)
            docRef.get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        handleEmptyVideoList() // No Level 1 folders
                        return@addOnSuccessListener
                    }

                    for (level1Doc in documents) {
                        val pdfURL = level1Doc.getString("pdfURL") ?: ""
                        val pdfTitle = level1Doc.getString("pdfTitle") ?: ""
                        val pdfBy = level1Doc.getString("pdfBy") ?: ""
                        val description = level1Doc.getString("description") ?: ""

                        val pdfs = PDFModel(
                            level1Doc.id,
                            description,
                            pdfBy,
                            pdfURL,
                            pdfTitle
                        )
                        pdfDetailsList.add(pdfs)
                        updateUIAfterDataFetch()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Error fetching guest pdf: $exception")
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
            .setMessage("Are you sure you want to delete this PDF?")
            .setPositiveButton("Delete") { _, _ ->
                deleteGuestPDFItem(pdfDetails!!)
            }
            .setNegativeButton("Cancel", null)
            .show()
        true
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun deleteGuestPDFItem(pdf: PDFModel) {
        showProgDialog()
        val docRef =  db.collection(AppConstants.TABLE_EDUCATION_PDF_CONTENT_GUEST).document(pdf.id!!)
        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Deleted ${pdf.pdfTitle}", Toast.LENGTH_SHORT).show()
               fetchAllGuestPDF()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to delete item", Toast.LENGTH_SHORT).show()
                dismissProgDialog()
            }
    }
}