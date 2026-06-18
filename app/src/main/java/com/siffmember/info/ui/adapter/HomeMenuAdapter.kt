package com.siffmember.info.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.activity.HomeActivity.Companion.TAG
import com.siffmember.info.ui.model.MenuDetails

class HomeMenuAdapter(private val menuItems: List<MenuDetails>, private val onSelectListener: HomeMenuListener) : RecyclerView.Adapter<HomeMenuAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_grid, parent, false)
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
        val mTitle: TextView = mView.findViewById(R.id.menu_title)
        val mIcon: ImageView = mView.findViewById(R.id.menu_image)
        val views: CardView = mView.findViewById(R.id.menu_items)

        fun bind(itemData: MenuDetails){
            mTitle.text = itemData.title
            mIcon.setImageResource(itemData.imageResId)
            views.setOnClickListener {
                onSelectListener.onMenuSelectListener(itemData)
            }
        }
    }

    interface HomeMenuListener {
        fun onMenuSelectListener(menuDetails: MenuDetails)
    }
}


