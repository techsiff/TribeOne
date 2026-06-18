package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.MeetingAnalyticsModel
import com.siffmember.info.ui.model.MembersZoomMeeting
import com.siffmember.info.utils.Utils

class MeetingsAnalyticsNonUsersAdapter(private var calls: List<MembersZoomMeeting>) : RecyclerView.Adapter<MeetingsAnalyticsNonUsersAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_mettings_non_users, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(calls[position])
    }

    override fun getItemCount(): Int = calls.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<MembersZoomMeeting>) {
        calls = newItems
        notifyDataSetChanged()
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.meeting_users)
        private val userPhone: TextView = itemView.findViewById(R.id.meeting_user_phone)

        @SuppressLint("SetTextI18n")
        fun bind(logs: MembersZoomMeeting) {
            userName.text = logs.name
            userPhone.text = logs.id
        }
    }
}