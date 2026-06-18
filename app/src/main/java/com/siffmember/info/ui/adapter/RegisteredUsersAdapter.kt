package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.UsersRegistration

class RegisteredUsersAdapter(
    private var items: List<UsersRegistration>,
    private val listener: UsersRegistrationListener
) : RecyclerView.Adapter<RegisteredUsersAdapter.ViewHolder>() {

    interface UsersRegistrationListener {
        fun onSelectUser(user: UsersRegistration?)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<UsersRegistration>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_manage_users, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
         val item = items[position]
        holder.bind(item)
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.user_name)
        private val phone: TextView = view.findViewById(R.id.user_phone)
        private val card: LinearLayout = view.findViewById(R.id.users_ll)

        fun bind(user: UsersRegistration) {
            name.text = user.name
            phone.text = user.phone_number
            card.setOnClickListener {
                listener.onSelectUser(user)
            }
        }
    }

}


