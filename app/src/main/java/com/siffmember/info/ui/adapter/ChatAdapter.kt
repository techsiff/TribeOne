package com.siffmember.info.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.MessageAI

class ChatAdapter(private val messages: List<MessageAI>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
        private const val VIEW_TYPE_TYPING = 3
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.isTyping -> VIEW_TYPE_TYPING
            message.isUser -> VIEW_TYPE_USER
            else -> VIEW_TYPE_BOT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_USER -> UserViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_sent, parent, false))
            VIEW_TYPE_TYPING -> TypingViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_typing, parent, false))
            else ->
            BotViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_message_received, parent, false))
        }
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]

        when (holder) {
            is UserViewHolder -> holder.bind(message)
            is BotViewHolder -> holder.bind(message)
            is TypingViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount() = messages.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textMessage)
        fun bind(message: MessageAI) {
            textView.text = message.text
        }
    }

    class BotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textView: TextView = itemView.findViewById(R.id.textMessage)
        fun bind(message: MessageAI) {
            textView.text = message.text
        }
    }

    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val typingText: TextView = itemView.findViewById(R.id.textTyping)

        fun bind(message: MessageAI) {
            typingText.text = message.text
        }
    }
}