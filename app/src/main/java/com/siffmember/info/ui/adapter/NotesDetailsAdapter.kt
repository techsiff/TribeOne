package com.siffmember.info.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.NotesDetails
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NotesDetailsAdapter(private val notes: List<NotesDetails>, private val deleteListener: DeleteListener) : RecyclerView.Adapter<NotesDetailsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_notes_details, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val dateTime = formatTime(notes[position].dateTime!!.toLong(), "MMM dd, yyyy HH:mm:ss")

        holder.mNotesDateTime.text = dateTime
        holder.mNotes.text = notes[position].notes

        holder.mDeleteNotes.setOnClickListener {
            deleteListener.onDeleteDevice(notes[position])
        }
    }

    override fun getItemCount(): Int {
        return notes.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mNotesDateTime: TextView = mView.findViewById<View>(R.id.notes_date_time) as TextView
        val mNotes: TextView = mView.findViewById<View>(R.id.notes_txt) as TextView
        val mDeleteNotes: ImageView = mView.findViewById<View>(R.id.delete_notes) as ImageView
    }

    private fun formatTime(millis: Long, pattern: String): String {
        val sdf = SimpleDateFormat(pattern, Locale.getDefault())
        val date = Date(millis)
        return sdf.format(date)
    }

    interface DeleteListener {
        fun onDeleteDevice(notesDetails: NotesDetails?)
    }
}


