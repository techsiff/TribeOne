package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R

class UserBlockAdapter(private var calls: List<String>, private val userBlockListener: UserBlockingListener) : RecyclerView.Adapter<UserBlockAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_user_block, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(calls[position], userBlockListener)
    }

    override fun getItemCount(): Int = calls.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<String>) {
        calls = newItems
        notifyDataSetChanged()
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val topic: TextView = itemView.findViewById(R.id.meeting_topic)
        private val btnJoin: LinearLayout = itemView.findViewById(R.id.meeting_join)

        @SuppressLint("SetTextI18n")
        fun bind(logs: String, meetingListener: UserBlockingListener) {
            topic.text = logs

            btnJoin.setOnClickListener {
                meetingListener.onUserBlock(logs)
            }
        }
    }

    interface UserBlockingListener {
        fun onUserBlock(blockTopic: String?)
    }
}