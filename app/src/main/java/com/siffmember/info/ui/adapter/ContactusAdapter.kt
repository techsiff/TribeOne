package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.ContactListItem
import com.siffmember.info.ui.model.ContactusDetails

class ContactusAdapter(
    private var items: List<ContactListItem>,
    private val listener: ContactListener,
    private val isAdmin: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CONTACT = 1
    }

    interface ContactListener {
        fun onContact(contactDetails: ContactusDetails?)
        fun onDeleteContact(contactDetails: ContactusDetails?)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<ContactListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ContactListItem.Header -> VIEW_TYPE_HEADER
            is ContactListItem.ContactItem -> VIEW_TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_header_title, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_contact_us, parent, false)
            ContactViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ContactListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is ContactListItem.ContactItem -> (holder as ContactViewHolder).bind(item.contact)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.user_name)
        private val phone: TextView = view.findViewById(R.id.user_phone)
        private val card: LinearLayout = view.findViewById(R.id.users_ll)
        private val delete: ImageView = view.findViewById(R.id.delete_contact)

        fun bind(contact: ContactusDetails) {
            name.text = contact.name
            phone.text = contact.phoneNumber
            card.setOnClickListener {
                listener.onContact(contact)
            }
            if(isAdmin){
                delete.visibility = View.VISIBLE
            } else {
                delete.visibility = View.GONE
            }
            delete.setOnClickListener {
                listener.onDeleteContact(contact)
            }
        }
    }

    inner class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerText: TextView = view.findViewById(R.id.header_title)

        fun bind(header: ContactListItem.Header) {
            headerText.text = header.state
        }
    }

}


