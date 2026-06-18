package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.siffmember.info.R
import com.siffmember.info.databinding.ItemCommunityHomeBinding
import com.siffmember.info.ui.model.CommunityModel
import com.siffmember.info.utils.Utils

class CommunityAdapter(private val currentUserId: String, private val currentUserName: String, val context: Context) : ListAdapter<CommunityModel, CommunityAdapter.CommunityViewHolder>(CommunityDiffCallback()) {

    var onItemClick: ((CommunityModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommunityViewHolder {
        val binding = ItemCommunityHomeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommunityViewHolder(binding, currentUserId, currentUserName, context)
    }

    override fun onBindViewHolder(holder: CommunityViewHolder, position: Int) {
        val community = getItem(position)
        holder.bind(community)
        holder.itemView.setOnClickListener { onItemClick?.invoke(community) }
    }

    class CommunityViewHolder(private val binding: ItemCommunityHomeBinding, private val currentUserId: String, private val currentUserName: String,val context: Context) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(community: CommunityModel) {
            binding.titleHomeTextView.text = community.groupName
            if(community.chats.userName.isNotEmpty()) {
                if (currentUserId == community.chats.userId) {
                    binding.sentUserName.text = "You : "
                } else {
                    binding.sentUserName.text = "${community.chats.userName} : "
                }
            }

            val content = community.chats.content
            val trimmedUser = currentUserName.trim()
            if (content.contains(trimmedUser, ignoreCase = true) && (content.contains("added", ignoreCase = true) || content.contains("removed", ignoreCase = true))) {
                val updatedContent = content.replace(trimmedUser, "You", ignoreCase = true)
                binding.lastSentMsg.text = updatedContent
            } else {
                binding.lastSentMsg.text = community.chats.content
            }
            try {
                if(community.chats.timestamp.isNotEmpty()) {
                    binding.textMessageTime.text =
                        Utils.getChatLastTime(community.chats.timestamp.toLong())
                }
            }catch (e: Exception){
                e.printStackTrace()
            }
            mSetPictureImageView(community.groupIcon)
        }

        private fun mSetPictureImageView(filePath: String){
            Glide.with(context)
                .load(filePath)
                .placeholder(R.drawable.ic_group_place_holder)
                .error(R.drawable.ic_group_place_holder)
                .apply(RequestOptions.circleCropTransform())
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(binding.groupsProfilePic)
        }
    }
}

class CommunityDiffCallback : DiffUtil.ItemCallback<CommunityModel>() {
    override fun areItemsTheSame(oldItem: CommunityModel, newItem: CommunityModel): Boolean {
        return oldItem.groupID == newItem.groupID
    }

    override fun areContentsTheSame(oldItem: CommunityModel, newItem: CommunityModel): Boolean {
        return oldItem == newItem
    }
}