package com.siffmember.info.ui.fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.siffmember.info.R
import com.siffmember.info.data.local.entity.CategoryTagEntity
import com.siffmember.info.data.local.entity.CommunityEntity
import com.siffmember.info.databinding.AddTagBottomSheetLayoutBinding
import com.siffmember.info.ui.activity.AddEducationPDFActivity
import com.siffmember.info.ui.adapter.CategorySpinnerAdapter
import com.siffmember.info.ui.view.ProgressDialog
import com.siffmember.info.ui.viewmodel.CommunityViewModel

class AddTagBottomSheetFragment : BottomSheetDialogFragment() {

    companion object {
        var TAG = "AddTagBottomSheetFragment"
        var ADD_CATEGORY = "Add new category"
        var CATEGORY_SELECT = "Select category"
    }

    private lateinit var binding: AddTagBottomSheetLayoutBinding
    private var progressDialog: ProgressDialog? = null
    private var categoryList = ArrayList<String>()
    private var spinnerAdapter: ArrayAdapter<String>? = null
    private var groupList = ArrayList<CommunityEntity>()
    private lateinit var customAdapter: CategorySpinnerAdapter
    private lateinit var viewModel: CommunityViewModel
    private var categoryNewTitle = false
    private var tagName = ""
    private var groupId = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = AddTagBottomSheetLayoutBinding.inflate(inflater, container, false)
        val root = binding.root

        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
       // setupAdapter()
        binding.apply {
            btnAddTag.setOnClickListener {
                try {
                    if(categoryNewTitle){
                        tagName = tagEditTxt.text.toString()
                    }
                    if(tagName.isEmpty()){
                        Toast.makeText(requireActivity(),"Select or enter category",Toast.LENGTH_LONG).show()
                    } else if(groupId.isEmpty()){
                        Toast.makeText(requireActivity(),"Select any one group",Toast.LENGTH_LONG).show()
                    } else {
                        viewModel.doesCategoryExist(tagName, groupId) { exists ->
                            if (exists) {
                                Toast.makeText(requireActivity(), "This category already exists for the selected group", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.getTotalCategoryCount { total ->
                                    if (total >= 20) {
                                        Toast.makeText(requireActivity(), "You can only add up to 20 categories", Toast.LENGTH_LONG).show()
                                    } else {
                                        viewModel.insertCategoryTag(CategoryTagEntity(0, groupId, tagName))
                                        dismiss()
                                    }
                                }
                            }
                        }
                    }
                }catch (e: Exception){
                    e.printStackTrace()
                }
            }

            spinnerGroups.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedGroup = groupList[position]
                    Log.d(TAG, "Category tags selected: $selectedGroup")
                    groupId = selectedGroup.groupID
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }

            spinnerTags.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                    val selectedCategory = categoryList[position]
                    if(selectedCategory == AddEducationPDFActivity.Companion.ADD_CATEGORY){
                        tagEditTxt.visibility = View.VISIBLE
                        categoryNewTitle = true
                    } else {
                        tagEditTxt.visibility = View.GONE
                        categoryNewTitle = false
                        if(selectedCategory != CATEGORY_SELECT) {
                            tagName = selectedCategory
                        }
                    }
                }

                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
        }

        viewModel.allCommunities.observe(viewLifecycleOwner) { community ->
            groupList.clear()
            groupList.addAll(community)

            Log.d(TAG, "Groups tags loaded: ${groupList.size}")
            setupGroupAdapter()
        }

        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            categoryList.clear()
            if(tags.isNotEmpty()) {
                categoryList.add(CATEGORY_SELECT)
                categoryList.addAll(tags)
                categoryList.add(ADD_CATEGORY)
                binding.spinnerTags.visibility = View.VISIBLE
                binding.tagEditTxt.visibility = View.GONE
                categoryNewTitle = false
            } else {
                binding.spinnerTags.visibility = View.GONE
                binding.tagEditTxt.visibility = View.VISIBLE
                categoryNewTitle = true
            }
            Log.d(TAG, "Category tags loaded: ${categoryList.size}")
            setupCategoryAdapter()
        }
    }

    private fun setupGroupAdapter() {
        customAdapter = CategorySpinnerAdapter(requireContext(), groupList).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spinnerGroups.adapter = it
        }
    }

    private fun setupCategoryAdapter() {
        spinnerAdapter = ArrayAdapter(requireActivity(), R.layout.spinner_list, categoryList).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spinnerTags.adapter = it
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
}
