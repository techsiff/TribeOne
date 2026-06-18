package com.siffmember.info.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.OpenPointList

class OpenPointsListAdapter(private val openPointsDetails: List<OpenPointList>, private val opListener: OpenPointsListener) : RecyclerView.Adapter<OpenPointsListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_open_points, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.mOPTitle.text = openPointsDetails[position].opName

        holder.mNextOP.setOnClickListener {
            opListener.onClickNext(openPointsDetails[position])
        }
    }

    override fun getItemCount(): Int {
        return openPointsDetails.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mOPTitle: TextView = mView.findViewById<View>(R.id.op_title) as TextView
        val mNextOP: CardView = mView.findViewById<View>(R.id.open_points_cv) as CardView
    }

    interface OpenPointsListener {
        fun onClickNext(opDetails: OpenPointList?)
    }
}

