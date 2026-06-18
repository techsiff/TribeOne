package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.GetUsers

class SearchUserAdapter(menuItems: List<GetUsers>, private val onSelectListener: CommunityUserListener) : RecyclerView.Adapter<SearchUserAdapter.ViewHolder>() {

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
        val mSelectUsers: CardView = mView.findViewById(R.id.users_cv)

        fun bind(itemData: GetUsers){
            mTitle.text = itemData.name
            mNumber.text = itemData.phone_number
            mUserSelect.visibility = View.GONE
            mSelectUsers.setOnClickListener {
                onSelectListener.onUserSelectListener(itemData, true)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<GetUsers>) {
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    interface CommunityUserListener {
        fun onUserSelectListener(users: GetUsers, isSelected: Boolean)
    }
}


