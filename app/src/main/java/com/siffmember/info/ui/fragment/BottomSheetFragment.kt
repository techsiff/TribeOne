package com.siffmember.info.ui.fragment

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObjects
import com.siffmember.info.R
import com.siffmember.info.databinding.BottomSheetLayoutBinding
import com.siffmember.info.ui.model.CategoryList
import com.siffmember.info.ui.view.ProgressDialog

class BottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "BottomSheetFragment"
        var CATEGORY_SELECT = "Select category"
        var SUB_CATEGORY_SELECT = "Select sub category"
    }

    private lateinit var binding: BottomSheetLayoutBinding
    private lateinit var db: FirebaseFirestore
    private var categoryOneList: ArrayList<String> = ArrayList()
    private var categoryTwoList: ArrayList<String> = ArrayList()
    private var progressDialog: ProgressDialog? = null
    private var listener: BottomSheetListener? = null
    private var categoryOne = ""
    private var categoryTwo = ""

    private var categoryFolderOne = "category_one"
    private var categoryFolderTwo = "category_two"

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // Ensure the parent activity implements the interface
        if (parentFragment is BottomSheetListener) {
            listener = parentFragment as BottomSheetListener
        } else {
            Log.e(TAG, "Parent Fragment must implement BottomSheetListener")
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = BottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        val type = arguments?.getString("type")
        if(type == "video"){
            categoryFolderOne = "category_one"
            categoryFolderTwo = "category_two"
        } else {
            categoryFolderOne = "category_one_pdf"
            categoryFolderTwo = "category_two_pdf"
        }

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = Firebase.firestore
        progressDialog = ProgressDialog(requireActivity())
        binding.apply {
            btnApplyFilter.setOnClickListener {
                try {
                    if(validate()){
                        listener!!.onApplyFilter(categoryOne, categoryTwo)
                        dismiss()
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

            spinnerCategory1.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (view != null) {
                        val category = categoryOneList[position]
                        if(category != CATEGORY_SELECT) {
                            categoryOne = category
                        }
                        getCategoryTwoDetails(category)
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
                        if (categorySub != SUB_CATEGORY_SELECT) {
                            categoryTwo = categorySub
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
        getCategoryOneDetails()
    }

    private fun validate(): Boolean {
        if (categoryOne.isEmpty()) {
            Toast.makeText(requireActivity(), "Please select category", Toast.LENGTH_LONG).show()
            return false
        }else if (categoryOne == CATEGORY_SELECT) {
            Toast.makeText(requireActivity(), "Please select category", Toast.LENGTH_LONG).show()
            return false
        }
        if (categoryTwo.isEmpty()) {
            Toast.makeText(requireActivity(), "Please select sub category", Toast.LENGTH_LONG).show()
            return false
        }else if (categoryTwo == SUB_CATEGORY_SELECT) {
            Toast.makeText(requireActivity(), "Please select sub category", Toast.LENGTH_LONG).show()
            return false
        }
        return true
    }

    private fun setupCategoryOneAdapter(){
        val adapter = ArrayAdapter(requireActivity(), R.layout.spinner_list, categoryOneList)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerCategory1.adapter = adapter
    }

    private fun setupCategoryTwoAdapter(){
        val adapter = ArrayAdapter(requireActivity(), R.layout.spinner_list, categoryTwoList)
        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        binding.spinnerCategory2.adapter = adapter
    }

    private fun getCategoryOneDetails(){
        try{
            showProgDialog()
            categoryOneList.clear()
            categoryOneList.add(CATEGORY_SELECT)
            val docRef = db.collection(categoryFolderOne)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        if(document.size() == 0) {
                            Log.e(TAG, "No such document")

                        } else {
                            Log.e(TAG, "DocumentSnapshot data: ${document.size()}")
                            val getList = document.toObjects<CategoryList>()
                            for(names in getList){
                                categoryOneList.add(names.name!!)
                            }
                            Log.e(TAG, "DocumentSnapshot data: ${categoryOneList.size}")
                        }
                    } else {
                        Log.e(TAG, "No such document")
                    }
                    setupCategoryOneAdapter()
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
            categoryTwoList.add(SUB_CATEGORY_SELECT)
            val docRef = db.collection(categoryFolderOne).document(category).collection(categoryFolderTwo)
            docRef.get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        if(document.size() == 0) {
                            Log.e(TAG, "No such document")
                        } else {
                            Log.e(TAG, "DocumentSnapshot data: ${document.size()}")
                            val getList = document.toObjects<CategoryList>()
                            for(names in getList){
                                categoryTwoList.add(names.name!!)
                            }
                            Log.e(TAG, "DocumentSnapshot data: ${categoryTwoList.size}")
                        }
                    } else {
                        Log.e(TAG, "No such document")
                    }
                    setupCategoryTwoAdapter()
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
        fun onApplyFilter(category1Folder: String, category2Folder: String)
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }
}
