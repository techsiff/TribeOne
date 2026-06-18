package com.siffmember.info.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R

class UserAccessAdapter(private val accessItems: List<String>, private val onSelectListener: UserAccessListener) : RecyclerView.Adapter<UserAccessAdapter.ViewHolder>() {

    private val selectedUsers = mutableSetOf<String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_user_access_details, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = accessItems[position]
        selectedUsers.add(accessItems[position])
        holder.bind(itemData, position)
    }

    override fun getItemCount(): Int {
        return accessItems.size
    }

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mTitle: TextView = mView.findViewById(R.id.user_access)
        val mUserSelect: CheckBox = mView.findViewById(R.id.user_checkbox)

        fun bind(itemData: String, position: Int){
            mTitle.text = itemData
            mUserSelect.isEnabled = position != 0
            mUserSelect.setOnCheckedChangeListener(null)
            mUserSelect.isChecked = selectedUsers.contains(itemData)
            mUserSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedUsers.add(itemData)
                } else {
                    selectedUsers.remove(itemData)
                }
                onSelectListener.onUserAccessSelectListener(itemData, isChecked)
            }
        }
    }

    interface UserAccessListener {
        fun onUserAccessSelectListener(users: String, isSelected: Boolean)
    }
}


