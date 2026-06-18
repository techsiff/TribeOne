package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.siffmember.info.R
import com.siffmember.info.ui.model.MeetingAnalyticsModel
import com.siffmember.info.ui.model.ParticipantModel
import com.siffmember.info.ui.model.SessionModel
import com.siffmember.info.utils.Utils

class MeetingsAnalyticsUsersAdapter(
    private var adminId: String, private var calls: List<ParticipantModel>
) : RecyclerView.Adapter<MeetingsAnalyticsUsersAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_mettings_users, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(calls[position], adminId)
    }

    override fun getItemCount(): Int = calls.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<ParticipantModel>) {
        calls = newItems
        notifyDataSetChanged()
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.meeting_users)
        private val userPhone: TextView = itemView.findViewById(R.id.meeting_user_phone)
        private val time: TextView = itemView.findViewById(R.id.meeting_joinedEnd_time)

        @SuppressLint("SetTextI18n")
        fun bind(logs: ParticipantModel, adminId: String) {
            if (adminId == logs.userId) {
                userName.text = "You"
            } else {
                userName.text = logs.userName
            }
            userPhone.text = logs.userId
            val totalDuration = logs.sessions.sumOf { session ->
                val join = session.joinTime?.toLongOrNull() ?: 0L
                val leave = session.leaveTime?.toLongOrNull() ?: join
                leave - join
            }
            Log.e("MeetingAnalytics","${logs.sessions}")
            if (logs.sessions.isNotEmpty()) {

                val firstJoinTime = logs.sessions.first().joinTime
                val lastLeaveTime = logs.sessions.last().leaveTime
                if (firstJoinTime != null && lastLeaveTime != null) {
                    time.text =
                        "StartTime: ${Utils.getChatTime(firstJoinTime.toLong())}\n" + "EndTime: ${
                            Utils.getChatTime(lastLeaveTime.toLong())
                        }\n" + "Duration: ${Utils.formatDuration(totalDuration)}"
                } else {
                    if(firstJoinTime != null){
                        time.text = "StartTime: ${Utils.getChatTime(firstJoinTime.toLong())}\n" + "EndTime: 0s\n" + "Duration: 0s"
                    } else {
                        time.text = "StartTime: 0s\n" + "EndTime: 0s\n" + "Duration: 0s"
                    }

                }
            } else {
                Log.e("MeetingAnalytics","${logs.userId}")
                time.text = "StartTime: 0s\n" + "EndTime: 0s\n" + "Duration: 0s"
            }
            itemView.setOnClickListener {
                showSessionDialog(
                    itemView.context,
                    logs.sessions,
                    totalDuration
                )
            }
        }

        private fun showSessionDialog(
            context: Context,
            sessions: List<SessionModel>,
            durationMillis: Long
        ) {

            if (sessions.isEmpty()) {
                Toast.makeText(
                    context,
                    "No session records found",
                    Toast.LENGTH_SHORT
                ).show()
                return
            }

            val message = buildString {

                sessions.sortedBy {
                    it.joinTime?.toLongOrNull() ?: 0L
                }.forEachIndexed { index, session ->
                    append("Session ${index + 1}\n")
                    append(
                        "Joined : ${
                            session.joinTime?.toLongOrNull()?.let {
                                Utils.getChatTime(it)
                            } ?: "-"
                        }\n"
                    )
                    append(
                        "Left   : ${
                            session.leaveTime?.toLongOrNull()?.let {
                                Utils.getChatTime(it)
                            } ?: "Still in meeting"
                        }\n"
                    )
                    append("\n")
                }
            }

            MaterialAlertDialogBuilder(context)
                .setTitle("Attended for ${Utils.formatDuration(durationMillis)}")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }
    }

}