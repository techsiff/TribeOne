package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.data.remote.model.community.GetDiscussion
import com.siffmember.info.utils.Utils

class DiscussionsAdapter(private val discussions: List<GetDiscussion>, private val discussionsListener: DiscussionsListener, private val user: String) :
    RecyclerView.Adapter<DiscussionsAdapter.DiscussionViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DiscussionViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_discussion, parent, false)
        return DiscussionViewHolder(view)
    }

    override fun onBindViewHolder(holder: DiscussionViewHolder, position: Int) {
        val discussion = discussions[position]
        holder.bind(discussion)
    }

    override fun getItemCount(): Int = discussions.size

    inner class DiscussionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleTextView: TextView = itemView.findViewById(R.id.titleTextView)
        private val contentTextView: TextView = itemView.findViewById(R.id.contentTextView)
        private val replyCount: TextView = itemView.findViewById(R.id.reply_count)
        private val userName: TextView = itemView.findViewById(R.id.post_user_name)
        private val time: TextView = itemView.findViewById(R.id.post_time)
        private val views: CardView = itemView.findViewById(R.id.cardViewDiscussion)

        @SuppressLint("SetTextI18n")
        fun bind(discussion: GetDiscussion) {
            titleTextView.text = discussion.title
            contentTextView.text = discussion.content
            if (user == discussion.userName){
                userName.text = "You"
            } else {
                userName.text = discussion.userName
            }
            time.text = Utils.getChatTime(discussion.timestamp)
            replyCount.text = Utils.formatDynamicCount(discussion.replies.size.toLong())
            views.setOnClickListener {
                discussionsListener.onClickNext(discussion)
            }
        }
    }

    interface DiscussionsListener {
        fun onClickNext(getDiscussion: GetDiscussion)
    }
}
