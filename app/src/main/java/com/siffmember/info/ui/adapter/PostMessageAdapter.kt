package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.data.local.entity.PostMessage
import com.siffmember.info.utils.Utils

class PostMessageAdapter(
    private val chatItems: List<ChatItem>,
    private val discussionsListener: DiscussionsListener,
    private val user: String,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_DATE_HEADER -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_date_header, parent, false)
                DateHeaderViewHolder(view)
            }
            TYPE_MESSAGE -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_discussion, parent, false)
                DiscussionViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = chatItems[position]) {
            is ChatItem.DateHeader -> (holder as DateHeaderViewHolder).bind(item.date)
            is ChatItem.Message -> (holder as DiscussionViewHolder).bind(item.message, item.replyCount)
        }
    }

    override fun getItemCount(): Int = chatItems.size

    override fun getItemViewType(position: Int): Int {
        return when (chatItems[position]) {
            is ChatItem.DateHeader -> TYPE_DATE_HEADER
            is ChatItem.Message -> TYPE_MESSAGE
        }
    }

    inner class DiscussionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val replyCountTextView: TextView = itemView.findViewById(R.id.reply_count)
        private val userName: TextView = itemView.findViewById(R.id.post_user_name)
        private val time: TextView = itemView.findViewById(R.id.post_time)
        private val views: CardView = itemView.findViewById(R.id.cardViewDiscussion)

        @SuppressLint("SetTextI18n")
        fun bind(discussion: PostMessage, replyCount: Int) {

             if (user == discussion.userName) {
                 userName.text = "You"
                 titleTextView.text = discussion.postTitle
                 contentTextView.text = discussion.content
            } else {
                 userName.text = discussion.userName
                 //titleTextView.text = discussion.postTitle
                 //contentTextView.text = discussion.content

                 val title = discussion.postTitle.trim()
                 val content = discussion.content.trim()
                 val trimmedUser = user.trim()

                 if (content.contains(trimmedUser, ignoreCase = true) && (content.contains("added", ignoreCase = true) || content.contains("removed", ignoreCase = true))) {
                     val updatedContent = content.replace(trimmedUser, "You", ignoreCase = true)
                     contentTextView.text = updatedContent
                 } else {
                     contentTextView.text = content
                 }
                 if (title.contains(trimmedUser, ignoreCase = true) && (title.contains("added", ignoreCase = true) || title.contains("removed", ignoreCase = true))) {
                     val updatedTitle = title.replace(trimmedUser, "You", ignoreCase = true)
                     titleTextView.text = updatedTitle
                 } else {
                     titleTextView.text = title
                 }
            }

            if (discussion.timestamp.isNotEmpty()) {
                time.text = Utils.getChatTime(discussion.timestamp.toLong())
            }

            replyCountTextView.text = Utils.formatDynamicCount(replyCount.toLong())

            views.setOnClickListener {
                discussionsListener.onClickNext(discussion)
            }
        }
    }

    inner class DateHeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.textChatDateHeader)
        fun bind(date: String) {
            dateTextView.text = date
        }
    }

    interface DiscussionsListener {
        fun onClickNext(getDiscussion: PostMessage)
    }

    sealed class ChatItem {
        data class DateHeader(val date: String) : ChatItem()
        data class Message(val message: PostMessage, val replyCount: Int) : ChatItem()
    }

    companion object {
        private const val TYPE_DATE_HEADER = 0
        private const val TYPE_MESSAGE = 1
    }
}