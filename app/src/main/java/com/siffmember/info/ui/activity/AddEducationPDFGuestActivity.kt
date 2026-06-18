package com.siffmember.info.ui.activity

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.ActivityAddEducationPdfGuestBinding
import com.siffmember.info.ui.model.PDFModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails

class AddEducationPDFGuestActivity : BaseActivity() {

    companion object {
        var TAG = "AddEducationPDFGuestActivity"
    }

    private lateinit var binding: ActivityAddEducationPdfGuestBinding
    private lateinit var db: FirebaseFirestore

    private var pdfURL = ""
    private var pdfTitle = ""
    private var pdfBy = ""
    private var pdfDescription = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEducationPdfGuestBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.appHeader) { v, insets ->
            val status = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            v.setPadding(0, status, 0, 0)
            insets
        }
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { view, insets ->

            val ime = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            val nav = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            // Push entire layout up
            view.setPadding(0, 0, 0, maxOf(ime, nav))
            insets
        }
        db = Firebase.firestore

        binding.apply {
            btnAdd.setOnClickListener {
                if(validate()) {
                    showProgDialog()
                    addVideo()
                }
            }
        }
    }

    private fun validate(): Boolean{

        if(binding.pdfUrlEdit.text.toString().trim().isNotEmpty()){
            if (Patterns.WEB_URL.matcher(binding.pdfUrlEdit.text.toString()).matches() && binding.pdfUrlEdit.text.toString().endsWith(".pdf", ignoreCase = true)) {
                pdfURL = binding.pdfUrlEdit.text.toString()
            } else {
                Toast.makeText(this@AddEducationPDFGuestActivity,"The PDF URL is not valid. Please enter a valid URL.", Toast.LENGTH_LONG).show()
                return false
            }
        } else {
            Toast.makeText(this@AddEducationPDFGuestActivity,"Please enter pdf URL", Toast.LENGTH_LONG).show()
            return false
        }

        if(binding.pdfTitleEdit.text.toString().trim().isNotEmpty()){
            pdfTitle = binding.pdfTitleEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationPDFGuestActivity,"Please enter pdf title", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.pdfByEdit.text.toString().trim().isNotEmpty()){
            pdfBy = binding.pdfByEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationPDFGuestActivity,"Please enter pdf by", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.pdfDescEdit.text.toString().trim().isNotEmpty()){
            pdfDescription = binding.pdfDescEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationPDFGuestActivity,"Please enter pdf description", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun addVideo(){
        try{
            // Level 1 Folder: Free Trainings
            val docRef = db.collection(AppConstants.TABLE_EDUCATION_PDF_CONTENT_GUEST).document()
            val pdfData = PDFModel(docRef.id, pdfDescription, pdfBy, pdfURL, pdfTitle)

            docRef
                .set(pdfData)
                .addOnSuccessListener {
                    Log.d(TAG, "DocumentSnapshot successfully written!")
                    binding.pdfUrlEdit.setText("")
                    binding.pdfTitleEdit.setText("")
                    binding.pdfByEdit.setText("")
                    binding.pdfDescEdit.setText("")
                    pdfURL = ""
                    pdfTitle = ""
                    pdfBy = ""
                    pdfDescription = ""
                    dismissProgDialog()
                    EducationDetails.setPDFContentAdd(true)
                    Toast.makeText(this@AddEducationPDFGuestActivity,"Added successfully", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@AddEducationPDFGuestActivity,"Failed to add try again!", Toast.LENGTH_LONG).show()
                    dismissProgDialog()
                }
        }catch (e: Exception){
            dismissProgDialog()
            e.printStackTrace()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
         if (keyCode == KEYCODE_BACK) {
            finish()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }
}