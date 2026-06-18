package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.siffmember.info.R
import com.siffmember.info.ui.model.PlayLists
import com.siffmember.info.utils.Utils

class PlaylistsAdapter(private val mContext: Context, private val playLists: List<PlayLists>, private val videoDetailsListener: VideoDetailsListener) : RecyclerView.Adapter<PlaylistsAdapter.ViewHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_playlists, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = playLists[position]
        holder.mVideoTitle.text = video.name
        holder.mVideoBy.text = "at ${Utils.getChatLastTime(video.createdAt.toLong())}"
        Glide.with(mContext)
            .load(R.drawable.ic_video_folder)
            .transform(CenterCrop())
            .into(holder.mVideoThumbnail)
        holder.mNextVideo.setOnClickListener {
            videoDetailsListener.onClickNext(video)
        }
    }

    override fun getItemCount(): Int {
        return playLists.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mVideoTitle: TextView = mView.findViewById<View>(R.id.playlist_title) as TextView
        val mVideoThumbnail: ImageView = mView.findViewById<View>(R.id.playlist_thumbnail) as ImageView
        val mVideoBy: TextView = mView.findViewById<View>(R.id.playlist_at) as TextView
        val mNextVideo: CardView = mView.findViewById<View>(R.id.playlist_cv) as CardView
    }

    interface VideoDetailsListener {
        fun onClickNext(playLists: PlayLists)
    }
}

