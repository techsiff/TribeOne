package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.OpenPointDetails

class OpenPointDetailsAdapter(private val openPointsDetails: List<OpenPointDetails>, private val opListener: OpenPointDetailListener) : RecyclerView.Adapter<OpenPointDetailsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_open_point_detail, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        holder.mOPTitle.text = openPointsDetails[position].description
        holder.mOPWR.text = "${openPointsDetails[position].whoWillDoIt} | ${openPointsDetails[position].remarks}"

        holder.mNextOP.setOnClickListener {
            opListener.onClickNext(openPointsDetails[position])
        }
    }

    override fun getItemCount(): Int {
        return openPointsDetails.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mOPTitle: TextView = mView.findViewById<View>(R.id.op_detail_title) as TextView
        val mOPWR: TextView = mView.findViewById<View>(R.id.op_detail_wr) as TextView
        val mNextOP: CardView = mView.findViewById<View>(R.id.open_point_detail_cv) as CardView
    }

    interface OpenPointDetailListener {
        fun onClickNext(opDetails: OpenPointDetails?)
    }
}

