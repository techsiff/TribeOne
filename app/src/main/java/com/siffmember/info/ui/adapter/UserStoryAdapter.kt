package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.databinding.ItemUserStoryBinding
import com.siffmember.info.ui.model.UserStoryModel

class UserStoryAdapter(
    private val list: ArrayList<UserStoryModel>,
    private val listener: UserStoryListener
) : RecyclerView.Adapter<UserStoryAdapter.ViewHolder>() {

    interface UserStoryListener {
        fun onDeleteUser(userStoryModel: UserStoryModel)
        fun onSelectUser(userStoryModel: UserStoryModel)
    }

    class ViewHolder(val binding: ItemUserStoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemUserStoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int = list.size

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.binding.tvUserName.text = item.userName
        holder.binding.tvUserId.text = item.userId
        holder.binding.tvStoryCount.text = "Story Count: ${item.storyList.size}"

        holder.binding.root.setOnClickListener {
            listener.onSelectUser(item)
        }

        holder.binding.ivDelete.setOnClickListener {
            listener.onDeleteUser(item)
        }
    }
}