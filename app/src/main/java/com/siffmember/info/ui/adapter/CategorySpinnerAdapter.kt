package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import com.siffmember.info.R
import com.siffmember.info.data.local.entity.CommunityEntity

class CategorySpinnerAdapter(
    private val context: Context,
    private val items: List<CommunityEntity>
) : ArrayAdapter<CommunityEntity>(context, R.layout.spinner_list_cust, items) {

    @SuppressLint("ViewHolder")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(R.layout.spinner_list_cust, parent, false)
        val textView = view.findViewById<TextView>(R.id.spinnerItemText)
        textView.text = items[position].groupName
        return view
    }

    @SuppressLint("MissingInflatedId")
    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = LayoutInflater.from(context).inflate(R.layout.spinner_dropdown_item, parent, false)
        val textView = view.findViewById<TextView>(R.id.dropdownItemText)
        textView.text = items[position].groupName
        return view
    }
}