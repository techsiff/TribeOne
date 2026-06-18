package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.MembersGroup

class CommunityUserGroupAdapter(var isGroupAdmin: Boolean, var adminId: String, private val menuItems: ArrayList<MembersGroup>, private val onSelectListener: CommunityUserGroupListener) : RecyclerView.Adapter<CommunityUserGroupAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_user_group_details, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val itemData = menuItems[position]
        holder.bind(itemData)
    }

    override fun getItemCount(): Int {
        return menuItems.size
    }

    inner class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mTitle: TextView = mView.findViewById(R.id.user_name)
        val mNumber: TextView = mView.findViewById(R.id.user_phone)
        val mUserCard: CardView = mView.findViewById(R.id.users_cv)
        val mDelete: ImageView = mView.findViewById(R.id.delete_member)

        @SuppressLint("SetTextI18n")
        fun bind(itemData: MembersGroup){
            if(adminId == itemData.phoneNumber){
                mTitle.text = "You"
            } else {
                mTitle.text = itemData.name
            }

            if(isGroupAdmin){
                if(adminId == itemData.phoneNumber){
                    mDelete.visibility = View.GONE
                } else {
                    mDelete.visibility = View.VISIBLE
                }
            } else {
                mDelete.visibility = View.GONE
            }
            mNumber.text = itemData.phoneNumber
            mUserCard.setOnClickListener{
                if(isGroupAdmin) {
                    onSelectListener.onUserSelectListener(itemData)
                }
            }
        }
    }

    interface CommunityUserGroupListener {
        fun onUserSelectListener(users: MembersGroup)
    }
}


