package com.siffmember.info.ui.fragment

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.R
import com.siffmember.info.databinding.FragmentCategoryBinding
import com.siffmember.info.ui.activity.CommunityPostsActivity
import com.siffmember.info.ui.adapter.CommunityAdapter
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat

class CategoryFragment : BaseFragment() {

    companion object {
        private const val TAG = "CategoryFragment"
        var CATEGORY_SELECT = "Select category"
    }

    private lateinit var binding: FragmentCategoryBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var viewModel: CommunityViewModel
    private lateinit var adapter: CommunityAdapter
    private lateinit var chatViewModel: PostsMessageViewModel
    private var categoryList = ArrayList<String>()
    private var spinnerAdapter: ArrayAdapter<String>? = null
    private var selectedTag = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCategoryBinding.inflate(inflater, container, false)
        val root = binding.root

        db = Firebase.firestore
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        chatViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        CommunityChat.setIsChatOpen(false)

        setupRecyclerView()
        setupObservers()

        return root
    }

    private fun setupRecyclerView() {
        adapter = CommunityAdapter(sharedPref.getString(AppConstants.USER_ID, null)!!, sharedPref.getString(AppConstants.USER_NAME, null)!!, requireActivity())
        binding.communityRv.layoutManager = LinearLayoutManager(requireActivity())
        binding.communityRv.adapter = adapter

        adapter.onItemClick = { community ->
            val intent = Intent(requireActivity(), CommunityPostsActivity::class.java).apply {
                putExtra(AppConstants.COMMUNITY_GROUP_ID, community.groupID)
                putExtra(AppConstants.COMMUNITY_GROUP_NAME, community.groupName)
            }
            startActivity(intent)
        }

        binding.spinnerCategory.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedTag = categoryList[position]
                viewModel.fetchGroupsWithLastPostByTag(chatViewModel, selectedTag)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                //
            }
        }

        binding.addGroupTagBtn.setOnClickListener {
            val bottomSheetFragment = AddTagBottomSheetFragment()
            bottomSheetFragment.show(childFragmentManager, bottomSheetFragment.tag)
        }

        binding.deleteTag.setOnClickListener {
            if(selectedTag != CATEGORY_SELECT){
                deleteTagDialog()
            }
        }
    }

    private fun setupObservers() {
        viewModel.groupsWithLastPost.observe(viewLifecycleOwner) { groups ->
            val distinctGroups = groups.distinctBy { it.groupID }
            if (distinctGroups.isEmpty()) {
                binding.noCommunity.visibility = View.VISIBLE
                binding.communityRv.visibility = View.GONE
            } else {
                binding.noCommunity.visibility = View.GONE
                binding.communityRv.visibility = View.VISIBLE
                adapter.submitList(distinctGroups)
            }
        }

        viewModel.allTags.observe(viewLifecycleOwner) { tags ->
            categoryList.clear()
            if(tags.isNotEmpty()){
                categoryList.addAll(tags)
                binding.deleteTag.visibility = View.VISIBLE
                binding.spinnerCategory.visibility = View.VISIBLE
            } else {
                categoryList.add(CATEGORY_SELECT)
                binding.deleteTag.visibility = View.GONE
                binding.spinnerCategory.visibility = View.GONE
            }
            Log.e(TAG, "Category tags loaded: ${categoryList.size}")
            setupCategoryOneAdapter()
        }
    }

    private fun setupCategoryOneAdapter() {
        spinnerAdapter = ArrayAdapter(requireActivity(), R.layout.spinner_list, categoryList).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
            binding.spinnerCategory.adapter = it
        }
    }

    private fun deleteTagDialog(){
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Tag")
            .setMessage("Are you sure you want to delete the tag \"$selectedTag\"?")
            .setPositiveButton("Yes") { _, _ ->
                viewModel.deleteCategoryTagByName(selectedTag)
                Toast.makeText(requireContext(), "Tag deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

}