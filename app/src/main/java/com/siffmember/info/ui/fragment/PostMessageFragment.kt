package com.siffmember.info.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.siffmember.info.databinding.FragmentChatsBinding
import com.siffmember.info.ui.activity.CommunityPostsActivity
import com.siffmember.info.ui.adapter.CommunityAdapter
import com.siffmember.info.ui.viewmodel.CommunityViewModel
import com.siffmember.info.ui.viewmodel.PostsMessageViewModel
import com.siffmember.info.utils.AppConstants
import com.siffmember.info.utils.CommunityChat

class PostMessageFragment : BaseFragment() {

    companion object {
        var TAG = "PostMessageFragment"
    }
    private lateinit var binding: FragmentChatsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var viewModel: CommunityViewModel
    private lateinit var adapter: CommunityAdapter
    private lateinit var postsViewModel: PostsMessageViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChatsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        db = Firebase.firestore
        viewModel = ViewModelProvider(this)[CommunityViewModel::class.java]
        postsViewModel = ViewModelProvider(this)[PostsMessageViewModel::class.java]
        CommunityChat.setIsChatOpen(false)
        adapter = CommunityAdapter(sharedPref.getString(AppConstants.USER_ID, null)!!, sharedPref.getString(AppConstants.USER_NAME, null)!!, requireActivity())
        binding.communityRv.adapter = adapter
        binding.communityRv.layoutManager = LinearLayoutManager(requireActivity())

        adapter.onItemClick = { community ->
            val next = Intent(requireActivity(), CommunityPostsActivity::class.java)
            next.putExtra(AppConstants.COMMUNITY_GROUP_ID, community.groupID)
            next.putExtra(AppConstants.COMMUNITY_GROUP_NAME, community.groupName)
            startActivity(next)
        }

        viewModel.groupsWithLastPost.observe(requireActivity()) { groups ->
            val distinctGroups = groups.distinctBy { it.groupID }  // Ensure unique groups
            if (distinctGroups.isEmpty()) {
                binding.noCommunity.visibility = View.VISIBLE
                binding.communityRv.visibility = View.GONE
            } else {
                binding.noCommunity.visibility = View.GONE
                binding.communityRv.visibility = View.VISIBLE
                adapter.submitList(distinctGroups)
            }
        }

        return root
    }

    override fun onResume() {
        super.onResume()
        viewModel.fetchGroupsWithLastPost(postsViewModel)
    }
}