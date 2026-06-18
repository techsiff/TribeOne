package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.PlayLists

class AddVideoPlaylistAdapter(private val playLists: List<PlayLists>, private val videoDetailsListener: VideoDetailsListener) : RecyclerView.Adapter<AddVideoPlaylistAdapter.ViewHolder>() {

    private val selectedUsers = mutableSetOf<PlayLists>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_add_playlists, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = playLists[position]
        selectedUsers.add(video)
        holder.mVideoTitle.text = video.name

        holder.mPlaylistTitle.setOnCheckedChangeListener(null)
        //holder.mPlaylistTitle.isChecked = selectedUsers.contains(video)
        holder.mPlaylistTitle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedUsers.add(video)
            } else {
                selectedUsers.remove(video)
            }
            videoDetailsListener.onPlaylistSelectListener(video, isChecked)
        }
    }

    override fun getItemCount(): Int {
        return playLists.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mVideoTitle: TextView = mView.findViewById<View>(R.id.add_playlist_title) as TextView
        val mPlaylistTitle: CheckBox = mView.findViewById<View>(R.id.add_playlist_checkbox) as CheckBox
    }

    interface VideoDetailsListener {
        fun onPlaylistSelectListener(playLists: PlayLists, isSelected: Boolean)
    }
}

