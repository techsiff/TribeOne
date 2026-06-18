package com.siffmember.info.ui.activity

import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.ActivityAddEducationPdfBinding
import com.siffmember.info.ui.adapter.LevelOneSpinnerAdapter
import com.siffmember.info.ui.fragment.UserAccessBottomSheetFragment
import com.siffmember.info.ui.model.CategoryList
import com.siffmember.info.ui.model.EducationContentAccess
import com.siffmember.info.ui.model.PDFModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.EducationDetails
import com.siffmember.info.utils.Utils

class AddEducationPDFActivity : BaseActivity(), UserAccessBottomSheetFragment.BottomSheetListener {

    companion object {
        var TAG = "AddEducationPDFActivity"
        var CATEGORY_SELECT = "Select category"
        var SUB_CATEGORY_SELECT = "Select sub category"
        var ADD_CATEGORY = "Add new category"
        var ADD_SUB_CATEGORY = "Add new sub category"
    }

    private lateinit var binding: ActivityAddEducationPdfBinding
    private lateinit var db: FirebaseFirestore

    private var pdfURL = ""
    private var pdfTitle = ""
    private var pdfBy = ""
    private var pdfDescription = ""
    private var categoryOneTitle = ""
    private var categoryOneTitleId = ""
    private var categoryOneTitleBy = ""
    private var categoryTwoTitle = ""
    private var categoryTwoTitleId = ""
    private var categoryTwoTitleBy = ""
    private var categoryOneNewTitle = false
    private var categoryTwoNewTitle = false
    private var categoryOneList: ArrayList<CategoryList> = ArrayList()
    private var categoryTwoList: ArrayList<CategoryList> = ArrayList()
    private var selectedUserAccessList: ArrayList<String> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEducationPdfBinding.inflate(layoutInflater)
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
                    if(categoryOneNewTitle){
                        val bottomSheetFragment = UserAccessBottomSheetFragment()
                        bottomSheetFragment.show(supportFragmentManager, bottomSheetFragment.tag)
                    } else {
                        showProgDialog()
                        addVideo()
                    }
                }
            }

            binding.spinnerCategory2.visibility = View.GONE
            spinnerCategory1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (view != null) {
                        val category = categoryOneList[position]
                        Log.e(AddEducationVideoActivity.Companion.TAG, "categoryOneTitleId: ${category.name}")
                        if(category.name == ADD_CATEGORY){
                            binding.category1EditTxt.visibility = View.VISIBLE
                            binding.category2EditTxt.visibility = View.VISIBLE
                            binding.spinnerCategory2.visibility = View.GONE
                            categoryOneNewTitle = true
                            categoryTwoNewTitle = true
                        } else if(category.name != CATEGORY_SELECT) {
                            binding.category1EditTxt.visibility = View.GONE
                            binding.category2EditTxt.visibility = View.GONE
                            binding.spinnerCategory2.visibility = View.VISIBLE
                            categoryOneNewTitle = false
                            categoryTwoNewTitle = false
                            categoryOneTitle = category.name!!
                            categoryOneTitleId = category.levelId
                            Log.e(AddEducationVideoActivity.Companion.TAG, "categoryOneTitleId: $categoryOneTitleId")
                            getCategoryTwoDetails(category.levelId)
                        } else {
                            binding.category1EditTxt.visibility = View.GONE
                            binding.category2EditTxt.visibility = View.GONE
                            binding.spinnerCategory2.visibility = View.GONE
                        }

                    } else {
                        // handle the case where the view parameter is null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // write code to perform some action
                }
            }

            spinnerCategory2.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (view != null) {
                        val categorySub = categoryTwoList[position]
                        if(categorySub.name == ADD_SUB_CATEGORY){
                            binding.category2EditTxt.visibility = View.VISIBLE
                            categoryTwoNewTitle = true
                        } else {
                            binding.category2EditTxt.visibility = View.GONE
                            categoryTwoNewTitle = false
                            if(categorySub.name != SUB_CATEGORY_SELECT) {
                                categoryTwoTitle = categorySub.name!!
                                categoryTwoTitleId = categorySub.levelId
                            }
                        }
                    } else {
                        // handle the case where the view parameter is null
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // write code to perform some action
                }
            }

        }
        for (item in Utils.allowedRoles){
            selectedUserAccessList.add(item)
        }
        getCategoryOneDetails()
    }

    private fun setupCategoryOneAdapter(){
        val adapter = LevelOneSpinnerAdapter(this,  categoryOneList)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerCategory1.adapter = adapter
    }

    private fun setupCategoryTwoAdapter(){
        val adapter = LevelOneSpinnerAdapter(this, categoryTwoList)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerCategory2.adapter = adapter
        if(categoryOneNewTitle) {
            binding.spinnerCategory2.setSelection(1)
        }
    }

    private fun getCategoryOneDetails(){
        try{
            showProgDialog()
            categoryOneList.clear()
            categoryOneList.add(CategoryList(CATEGORY_SELECT))
            val docRef = db.collection(AppConstants.TABLE_EDUCATION_PDF_CONTENT)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        if(document.size() == 0) {
                            Log.e(TAG, "No such document")
                        } else {
                            Log.e(TAG, "DocumentSnapshot data: ${document.size()}")
                            for(doc in document){
                                val name = doc.getString("name") ?: ""
                                val allowedRoles = doc.get("allowedRoles") as? List<String> ?: emptyList()
                                categoryOneList.add(CategoryList(name, doc.id, "" ,allowedRoles))
                                Log.e(TAG, "allowedRoles data: ${allowedRoles.size}")
                            }
                            Log.d(TAG, "DocumentSnapshot data: ${categoryOneList.size}")
                        }
                    } else {
                        Log.e(TAG, "No such document")
                    }
                    runOnUiThread {
                        categoryOneList.add(CategoryList(ADD_CATEGORY))
                        setupCategoryOneAdapter()
                    }
                    dismissProgDialog()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "get failed with ", exception)
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun getCategoryTwoDetails(category: String){
        try{
            showProgDialog()
            categoryTwoList.clear()
            categoryTwoList.add(CategoryList(SUB_CATEGORY_SELECT))
            val docRef = db.collection(AppConstants.TABLE_EDUCATION_PDF_CONTENT).document(category).collection(
                AppConstants.TABLE_LEVEL_TWO)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        if(document.size() == 0) {
                            Log.e(TAG, "No such document")
                        } else {
                            Log.e(TAG, "DocumentSnapshot data: ${document.size()}")
                            for(doc in document){
                                val name = doc.getString("name") ?: ""
                                categoryTwoList.add(CategoryList(name, doc.id))
                            }
                            Log.e(TAG, "DocumentSnapshot data: ${categoryTwoList.size}")
                        }
                    } else {
                        Log.e(TAG, "No such document")
                    }
                    runOnUiThread {
                        categoryTwoList.add(CategoryList(ADD_SUB_CATEGORY))
                        setupCategoryTwoAdapter()
                    }
                    dismissProgDialog()
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "get failed with ", exception)
                    dismissProgDialog()
                }
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun validate(): Boolean{

        if(categoryOneNewTitle){
            if(binding.category1EditTxt.text.toString().trim().isNotEmpty()){
                categoryOneTitle = binding.category1EditTxt.text.toString()
            } else {
                Toast.makeText(this@AddEducationPDFActivity,"Please enter category title", Toast.LENGTH_LONG).show()
                return false
            }
        } else {
            if(categoryOneTitle.isEmpty()){
                Toast.makeText(this@AddEducationPDFActivity,"Please select category", Toast.LENGTH_LONG).show()
                return false
            }
        }
        if(categoryTwoNewTitle){
            if(binding.category2EditTxt.text.toString().trim().isNotEmpty()){
                categoryTwoTitle = binding.category2EditTxt.text.toString()
            } else {
                Toast.makeText(this@AddEducationPDFActivity,"Please enter sub category title", Toast.LENGTH_LONG).show()
                return false
            }
        } else {
            if(categoryTwoTitle.isEmpty()){
                Toast.makeText(this@AddEducationPDFActivity,"Please select sub category", Toast.LENGTH_LONG).show()
                return false
            }
        }
        if(binding.pdfUrlEdit.text.toString().trim().isNotEmpty()){
            if (Patterns.WEB_URL.matcher(binding.pdfUrlEdit.text.toString()).matches() && binding.pdfUrlEdit.text.toString().endsWith(".pdf", ignoreCase = true)) {
                pdfURL = binding.pdfUrlEdit.text.toString()
            } else {
                Toast.makeText(this@AddEducationPDFActivity,"The PDF URL is not valid. Please enter a valid URL.", Toast.LENGTH_LONG).show()
                return false
            }
        } else {
            Toast.makeText(this@AddEducationPDFActivity,"Please enter pdf URL", Toast.LENGTH_LONG).show()
            return false
        }

        if(binding.pdfTitleEdit.text.toString().trim().isNotEmpty()){
            pdfTitle = binding.pdfTitleEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationPDFActivity,"Please enter pdf title", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.pdfByEdit.text.toString().trim().isNotEmpty()){
            pdfBy = binding.pdfByEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationPDFActivity,"Please enter pdf by", Toast.LENGTH_LONG).show()
            return false
        }
        if(binding.pdfDescEdit.text.toString().trim().isNotEmpty()){
            pdfDescription = binding.pdfDescEdit.text.toString()
        } else {
            Toast.makeText(this@AddEducationPDFActivity,"Please enter pdf description", Toast.LENGTH_LONG).show()
            return false
        }
        if(selectedUserAccessList.isEmpty()){
            Toast.makeText(this@AddEducationPDFActivity,"Please select user access", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun addVideo(){
        try{
            val levelOneRef: DocumentReference
            val levelOneId: String
            if(categoryOneTitleId.isNotEmpty()) {
                levelOneRef = db.collection(AppConstants.TABLE_EDUCATION_PDF_CONTENT).document(categoryOneTitleId)
                levelOneId = categoryOneTitleId
                categoryOneTitleBy = ""
            } else {
                levelOneRef = db.collection(AppConstants.TABLE_EDUCATION_PDF_CONTENT).document()
                levelOneId = levelOneRef.id
                categoryOneTitleBy = pdfBy
                val levelOneFolderName = EducationContentAccess(categoryOneTitle,categoryOneTitleBy,
                    selectedUserAccessList)
                levelOneRef.set(levelOneFolderName)
            }

            val levelTwoRef: DocumentReference
            val levelTwoId: String
            if(categoryTwoTitleId.isNotEmpty()){
                levelTwoRef = levelOneRef.collection(AppConstants.TABLE_LEVEL_TWO).document(categoryTwoTitleId)
                levelTwoId = categoryTwoTitleId
                categoryTwoTitleBy = ""
            } else {
                levelTwoRef = levelOneRef.collection(AppConstants.TABLE_LEVEL_TWO).document()
                levelTwoId = levelTwoRef.id
                categoryTwoTitleBy = pdfBy
                levelTwoRef.set(mapOf("name" to categoryTwoTitle, "createdBy" to categoryTwoTitleBy), SetOptions.merge())

            }

            val level3Ref = levelTwoRef.collection(AppConstants.TABLE_LEVEL_THREE).document()
            val pdfData = PDFModel(level3Ref.id, pdfDescription, pdfBy, pdfURL, pdfTitle, levelTwoId, levelOneId)

            level3Ref
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
                    categoryOneTitle = ""
                    categoryOneTitleId = ""
                    categoryOneTitleBy = ""
                    categoryTwoTitle = ""
                    categoryTwoTitleId = ""
                    categoryTwoTitleBy = ""
                    binding.category1EditTxt.setText("")
                    binding.category2EditTxt.setText("")
                    binding.spinnerCategory1.setSelection(0)
                    binding.spinnerCategory2.setSelection(0)
                    binding.category1EditTxt.visibility = View.GONE
                    binding.category2EditTxt.visibility = View.GONE
                    getCategoryOneDetails()
                    dismissProgDialog()
                    EducationDetails.setPDFContentAdd(true)
                    Toast.makeText(this@AddEducationPDFActivity,"Added successfully", Toast.LENGTH_LONG).show()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error writing document", e)
                    Toast.makeText(this@AddEducationPDFActivity,"Failed to add try again!", Toast.LENGTH_LONG).show()
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

    override fun onUserAccessMember(selectedUsersList: List<String>) {
        selectedUserAccessList = selectedUsersList as ArrayList<String>
        if(selectedUserAccessList.isEmpty()){
            Toast.makeText(this@AddEducationPDFActivity,"Please select user access", Toast.LENGTH_LONG).show()
        } else {
            showProgDialog()
            addVideo()
        }
    }
}