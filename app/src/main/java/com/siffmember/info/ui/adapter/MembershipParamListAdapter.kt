package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.MembershipParamField
import com.siffmember.info.ui.model.MembershipParamListItem

class MembershipParamListAdapter(
    private var items: List<MembershipParamListItem>,
    private val listener: ParameterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_CONTACT = 1
    }

    interface ParameterListener {
        fun onDeleteParameter(paramDetails: MembershipParamField?)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newItems: List<MembershipParamListItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is MembershipParamListItem.Header -> VIEW_TYPE_HEADER
            is MembershipParamListItem.ParamsItem -> VIEW_TYPE_CONTACT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_param_header_title, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_parameters, parent, false)
            ContactViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is MembershipParamListItem.Header -> (holder as HeaderViewHolder).bind(item)
            is MembershipParamListItem.ParamsItem -> (holder as ContactViewHolder).bind(item.params)
        }
    }

    override fun getItemCount(): Int = items.size

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val paramName: TextView = itemView.findViewById(R.id.param_name)
        private val btnDelete: ImageView = itemView.findViewById(R.id.paramDelete)

        fun bind(paramDetails: MembershipParamField) {
            paramName.text = paramDetails.paramName

            /*if(paramDetails.paramName.contentEquals("Membership Number")
                || paramDetails.paramName.contentEquals("Phone Number")
                || paramDetails.paramName.contentEquals("Address")
                || paramDetails.paramName.contentEquals("Name")
                || paramDetails.paramName.contentEquals("City")
                || paramDetails.paramName.contentEquals("Country Code")
                || paramDetails.paramName.contentEquals("Email")
                || paramDetails.paramName.contentEquals("Joining Date")){
                btnDelete.visibility = View.GONE
            } else {
                btnDelete.visibility = View.VISIBLE
            }*/
            if(paramDetails.paramName.contentEquals("Phone Number") || paramDetails.paramName.contentEquals("Country Code")){
                btnDelete.visibility = View.GONE
            } else {
                btnDelete.visibility = View.VISIBLE
            }
            btnDelete.setOnClickListener {
                listener.onDeleteParameter(paramDetails)
            }
        }
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val headerText: TextView = view.findViewById(R.id.header_title)

        fun bind(header: MembershipParamListItem.Header) {
            headerText.text = header.category
        }
    }

}


