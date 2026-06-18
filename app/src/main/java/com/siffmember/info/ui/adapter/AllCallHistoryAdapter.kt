package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.utils.Utils
import com.siffmember.info.ui.model.UpdateUsersCallLog

class AllCallHistoryAdapter(private var calls: List<UpdateUsersCallLog>) : RecyclerView.Adapter<AllCallHistoryAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_all_call_history, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(calls[position])
    }

    override fun getItemCount(): Int = calls.size

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<UpdateUsersCallLog>) {
        calls = newItems
        notifyDataSetChanged()
    }

    class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val fromUserName: TextView = itemView.findViewById(R.id.from_user_name)
        private val fromUserPhone: TextView = itemView.findViewById(R.id.from_user_phone)
        private val toUserName: TextView = itemView.findViewById(R.id.to_user_name)
        private val toUserPhone: TextView = itemView.findViewById(R.id.to_user_phone)
        private val time: TextView = itemView.findViewById(R.id.call_details)

        @SuppressLint("SetTextI18n")
        fun bind(logs: UpdateUsersCallLog) {
            fromUserName.text = logs.fromUserName
            fromUserPhone.text = logs.fromUserPhoneNumber
            toUserName.text = logs.toUserName
            toUserPhone.text = logs.toUserPhoneNumber
            time.text = "${Utils.getTimeAgo(logs.timestamp!!.toLong())}  •  ${Utils.formatCallDuration(logs.duration!!.toInt())}"
        }
    }
}