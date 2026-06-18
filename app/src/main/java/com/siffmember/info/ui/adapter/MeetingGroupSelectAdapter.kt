package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.data.local.entity.CommunityEntity

class MeetingGroupSelectAdapter(menuItems: List<CommunityEntity>, private val onSelectListener: GroupSelectedListener) : RecyclerView.Adapter<MeetingGroupSelectAdapter.ViewHolder>() {

    private val selectedUsers = mutableSetOf<CommunityEntity>()
    private var filteredList = menuItems.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_user_details, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = filteredList[position]
        holder.bind(itemData)
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mTitle: TextView = mView.findViewById(R.id.user_name)
        val mNumber: TextView = mView.findViewById(R.id.user_phone)
        val mUserSelect: CheckBox = mView.findViewById(R.id.user_checkbox)

        fun bind(itemData: CommunityEntity){
            mTitle.text = itemData.groupName
            mNumber.visibility = View.GONE

            mUserSelect.setOnCheckedChangeListener(null)
            mUserSelect.isChecked = selectedUsers.contains(itemData)
            mUserSelect.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedUsers.add(itemData)
                } else {
                    selectedUsers.remove(itemData)
                }
                onSelectListener.onGroupSelectListener(itemData, isChecked)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<CommunityEntity>) {
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    interface GroupSelectedListener {
        fun onGroupSelectListener(groups: CommunityEntity, isSelected: Boolean)
    }
}


