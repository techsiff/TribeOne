package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.UpdateCallLog
import com.siffmember.info.utils.Utils
import androidx.core.graphics.toColorInt

class CallHistoryAdapter(private val calls: List<UpdateCallLog>) : RecyclerView.Adapter<CallHistoryAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_call_history, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(calls[position])
    }

    override fun getItemCount(): Int = calls.size

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.user_name)
        private val userPhone: TextView = itemView.findViewById(R.id.user_phone)
        private val time: TextView = itemView.findViewById(R.id.call_details)
        private val type: ImageView = itemView.findViewById(R.id.type)

        @SuppressLint("SetTextI18n")
        fun bind(logs: UpdateCallLog) {
            userName.text = logs.name
            userPhone.text = logs.phoneNumber
            if (logs.timestamp!!.isNotEmpty()) {
                val callType = when (logs.type) {
                    "1" -> {
                        "Incoming Call"
                    }
                    "2" -> {
                        "Outgoing Call"
                    }
                    "3" -> {
                        "Missed Call"
                    }
                    "5" -> {
                        "Declined Call"
                    }
                    else -> {
                        ""
                    }
                }
                time.text = "$callType  •  ${Utils.getTimeAgo(logs.timestamp.toLong())}  •  ${Utils.formatCallDuration(logs.duration!!.toInt())}"
            }

            when (logs.type) {
                "1" -> {
                    type.setImageResource(R.drawable.ic_incoming)
                    time.setTextColor("#7C7272".toColorInt())
                }
                "2" -> {
                    type.setImageResource(R.drawable.ic_outgoing)
                    time.setTextColor("#7C7272".toColorInt())
                }
                "3" -> {
                    type.setImageResource(R.drawable.ic_missed)
                    time.setTextColor("#FF0000".toColorInt())
                }
                "5" -> {
                    type.setImageResource(R.drawable.ic_declined)
                    time.setTextColor("#FF0000".toColorInt())
                }
            }

        }
    }
}