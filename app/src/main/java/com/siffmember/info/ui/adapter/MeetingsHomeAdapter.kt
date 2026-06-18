package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.utils.Utils

class MeetingsHomeAdapter(private var calls: List<MeetingHomeDetailsModel>, private val meetingListener: MeetingsListener) : RecyclerView.Adapter<MeetingsHomeAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_mettings, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(calls[position], meetingListener)
    }

    override fun getItemCount(): Int = calls.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<MeetingHomeDetailsModel>) {
        calls = newItems
        notifyDataSetChanged()
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val topic: TextView = itemView.findViewById(R.id.meeting_topic)
        private val time: TextView = itemView.findViewById(R.id.meeting_time)
        private val host: TextView = itemView.findViewById(R.id.meeting_host)
        private val btnJoin: LinearLayout = itemView.findViewById(R.id.meeting_join)

        @SuppressLint("SetTextI18n")
        fun bind(logs: MeetingHomeDetailsModel, meetingListener: MeetingsListener) {
            topic.text = logs.roomName
            time.visibility = View.GONE
            host.visibility = View.GONE

            btnJoin.setOnClickListener {
                meetingListener.oSelectMeetingRoom(logs)
            }
        }
    }

    interface MeetingsListener {
        fun oSelectMeetingRoom(meetingDetails: MeetingHomeDetailsModel?)
    }
}