package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.util.Util
import com.siffmember.info.R
import com.siffmember.info.ui.model.MeetingAnalyticsModel
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.ui.model.MeetingHomeDetailsModel
import com.siffmember.info.utils.Utils

class MeetingsAnalyticsAdapter(private var calls: List<MeetingAnalyticsModel>, private val meetingListener: MeetingsListener) : RecyclerView.Adapter<MeetingsAnalyticsAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_mettings, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(calls[position], meetingListener)
    }

    override fun getItemCount(): Int = calls.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<MeetingAnalyticsModel>) {
        calls = newItems
        notifyDataSetChanged()
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val topic: TextView = itemView.findViewById(R.id.meeting_topic)
        private val time: TextView = itemView.findViewById(R.id.meeting_time)
        private val host: TextView = itemView.findViewById(R.id.meeting_host)
        private val btnJoin: LinearLayout = itemView.findViewById(R.id.meeting_join)

        @SuppressLint("SetTextI18n")
        fun bind(logs: MeetingAnalyticsModel, meetingListener: MeetingsListener) {
            topic.text = Utils.getMeetingDateTime(logs.startTime!!)
            if(logs.endTime != null && logs.startTime != null) {
                time.text = "Meeting Duration: ${Utils.getMeetingDuration(logs.startTime!!, logs.endTime!!)}"
            } else {
                time.text = "Meeting Duration: 0s"
            }
            host.visibility = View.GONE

            btnJoin.setOnClickListener {
                meetingListener.oSelectMeetingHistory(logs)
            }
        }
    }

    interface MeetingsListener {
        fun oSelectMeetingHistory(meetingDetails: MeetingAnalyticsModel?)
    }
}