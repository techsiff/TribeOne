package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.AnnouncementDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnnouncementAdapter(private val announce: List<AnnouncementDetails>, private val deleteListener: DeleteListener, private val category: String) : RecyclerView.Adapter<AnnouncementAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_announcement, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dateTime = formatTime(announce[position].dateTime!!.toLong(), "MMM dd, yyyy HH:mm:ss")

        val announceData = announce[position]
        holder.mDateTime.text = dateTime
        holder.mTitle.text = announceData.title
        holder.mDescription.text = announceData.description
        /*if(category == "Admin"){
            holder.mDeleteAnnounce.visibility = View.VISIBLE
        } else {
            holder.mDeleteAnnounce.visibility = View.GONE
        }*/
        holder.mDeleteAnnounce.setOnClickListener {
            deleteListener.onDeleteDevice(announceData)
        }
    }

    override fun getItemCount(): Int {
        return announce.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mDateTime: TextView = mView.findViewById<View>(R.id.announce_date_time) as TextView
        val mTitle: TextView = mView.findViewById<View>(R.id.announce_title) as TextView
        val mDescription: TextView = mView.findViewById<View>(R.id.announce_description) as TextView
        val mDeleteAnnounce: ImageView = mView.findViewById<View>(R.id.delete_announce) as ImageView
    }

    private fun formatTime(millis: Long, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val date = Date(millis)
        return sdf.format(date)
    }

    interface DeleteListener {
        fun onDeleteDevice(announceDetails: AnnouncementDetails?)
    }
}


