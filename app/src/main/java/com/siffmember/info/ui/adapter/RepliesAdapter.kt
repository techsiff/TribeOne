package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.data.remote.model.community.Reply
import com.siffmember.info.utils.Utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RepliesAdapter(private val replies: List<Reply>, private val user: String) : RecyclerView.Adapter<RepliesAdapter.ReplyViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        // Inflate the reply item layout (item_reply.xml)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reply, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        val reply = replies[position]
        holder.bind(reply)
    }

    override fun getItemCount(): Int = replies.size

    inner class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val replyContentTextView: TextView = itemView.findViewById(R.id.replyContentTextView)
        private val replyTimestampTextView: TextView = itemView.findViewById(R.id.replyTimestampTextView)
        private val userName: TextView = itemView.findViewById(R.id.reply_user_name)
        private val time: TextView = itemView.findViewById(R.id.reply_time)
        @SuppressLint("SetTextI18n")
        fun bind(reply: Reply) {
            replyContentTextView.text = reply.content
            if (user == reply.userName){
                userName.text = "You"
            } else {
                userName.text = reply.userName
            }
            time.text = Utils.getTimeAgo(reply.timestamp)

            // Format the timestamp (Optional)
            val formattedTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                Date(reply.timestamp)
            )
            replyTimestampTextView.text = formattedTimestamp
        }
    }
}
