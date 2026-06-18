package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.data.local.entity.ReplyPostMessage
import com.siffmember.info.utils.Utils
import java.text.SimpleDateFormat
import java.util.*

class ReplyPostMessageAdapter(
    private val replies: MutableList<ReplyPostMessage>,
    private val user: String,
    private val replyPostListener: ReplyPostListener,
    private val isAdmin: Boolean
) : RecyclerView.Adapter<ReplyPostMessageAdapter.ReplyViewHolder>() {

    private val selectedReplies = mutableSetOf<ReplyPostMessage>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReplyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reply, parent, false)
        return ReplyViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReplyViewHolder, position: Int) {
        holder.bind(replies[position])
    }

    override fun getItemCount(): Int = replies.size

    inner class ReplyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val replyContentTextView: TextView = itemView.findViewById(R.id.replyContentTextView)
        private val replyTimestampTextView: TextView = itemView.findViewById(R.id.replyTimestampTextView)
        private val userName: TextView = itemView.findViewById(R.id.reply_user_name)
        private val time: TextView = itemView.findViewById(R.id.reply_time)

        @SuppressLint("SetTextI18n")
        fun bind(reply: ReplyPostMessage) {
            replyContentTextView.text = reply.content

            userName.text = if (user == reply.userName) "You" else reply.userName

            if (reply.timestamp.isNotEmpty()) {
                time.text = Utils.getTimeAgo(reply.timestamp.toLong())
            }

            val formattedTimestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(
                Date(reply.timestamp.toLong())
            )
            replyTimestampTextView.text = formattedTimestamp

            // Highlight if selected
            itemView.setBackgroundColor(
                if (selectedReplies.contains(reply))
                    ContextCompat.getColor(itemView.context, R.color.selected_item_bg)
                else
                    ContextCompat.getColor(itemView.context, R.color.white)
            )

            if (isAdmin) {
                itemView.setOnLongClickListener {
                    toggleSelection(reply)
                    true
                }
            }

            itemView.setOnClickListener {
                if (selectedReplies.isNotEmpty()) {
                    toggleSelection(reply)
                } else {
                    replyPostListener.onClickNext(reply)
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun toggleSelection(reply: ReplyPostMessage) {
        if (selectedReplies.contains(reply)) {
            selectedReplies.remove(reply)
        } else {
            selectedReplies.add(reply)
        }
        notifyDataSetChanged()
        replyPostListener.onSelectionChanged(selectedReplies.toList())
    }

    interface ReplyPostListener {
        fun onClickNext(replyPostMessage: ReplyPostMessage)
        fun onSelectionChanged(selected: List<ReplyPostMessage>)
    }
}