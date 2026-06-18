package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.MeetingDetailsModel
import com.siffmember.info.utils.Utils
import androidx.core.graphics.toColorInt

class MeetingsAdapter(private var calls: List<MeetingDetailsModel>, private val meetingListener: MeetingsListener) : RecyclerView.Adapter<MeetingsAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_mettings, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(calls[position], meetingListener)
    }

    override fun getItemCount(): Int = calls.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<MeetingDetailsModel>) {
        calls = newItems
        notifyDataSetChanged()
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val topic: TextView = itemView.findViewById(R.id.meeting_topic)
        private val time: TextView = itemView.findViewById(R.id.meeting_time)
        private val host: TextView = itemView.findViewById(R.id.meeting_host)
        private val btnJoin: LinearLayout = itemView.findViewById(R.id.meeting_join)

        @SuppressLint("SetTextI18n")
        fun bind(logs: MeetingDetailsModel, meetingListener: MeetingsListener) {
            topic.text = logs.topicName
            time.text = "Time: ${Utils.getMeetingDateTime(logs.timestamp!!)}"
            if(logs.inMeeting){
                host.setTextColor("#079A4A".toColorInt())
                host.text = "Host: ${logs.hostName} Now In Meeting"
            } else {
                host.setTextColor("#7C7272".toColorInt())
                host.text = "Host: ${logs.hostName} "
            }


            btnJoin.setOnClickListener {
                meetingListener.onJoinMeeting(logs)
            }
        }
    }

    interface MeetingsListener {
        fun onJoinMeeting(meetingDetails: MeetingDetailsModel?)
    }
}