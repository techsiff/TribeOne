package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.MembersGroup

class UserTagAdapter(private val userList: ArrayList<MembersGroup>, private val onSelectListener: CommunityUserGroupListener) : RecyclerView.Adapter<UserTagAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_user_meeting_details, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = userList[position]
        holder.bind(itemData)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mTitle: TextView = mView.findViewById(R.id.user_name)
        val mNumber: TextView = mView.findViewById(R.id.user_phone)
        val mUserCard: CardView = mView.findViewById(R.id.users_cv)

        @SuppressLint("SetTextI18n")
        fun bind(itemData: MembersGroup){
            mTitle.text = itemData.name
            mNumber.text = itemData.phoneNumber
            mUserCard.setOnClickListener{
                onSelectListener.onUserSelectListener(itemData)
            }
        }
    }

    interface CommunityUserGroupListener {
        fun onUserSelectListener(users: MembersGroup)
    }
}


