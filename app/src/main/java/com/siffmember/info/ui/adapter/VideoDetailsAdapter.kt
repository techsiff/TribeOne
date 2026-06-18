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
import com.siffmember.info.ui.model.VideoModel

class VideoDetailsAdapter(private val mContext: Context, videoDetails: List<VideoModel>, private val videoDetailsListener: VideoDetailsListener, private val isAdmin: Boolean) : RecyclerView.Adapter<VideoDetailsAdapter.ViewHolder>() {

    private var filteredList = videoDetails.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_video_details, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = filteredList[position]
        holder.mVideoTitle.text = video.videoTitle
        holder.mVideoBy.text = "by ${video.videoBy}"
        holder.mVideoDuration.text = video.videoDuration
        holder.mVideoThumbnail.visibility = View.VISIBLE
        Glide.with(mContext)
            .load(video.videoThumbnail)
            .transform(CenterCrop())
            .into(holder.mVideoThumbnail)
        holder.mNextVideo.setOnClickListener {
            videoDetailsListener.onClickNext(video)
        }
        holder.mDeleteVideo.setOnClickListener {
            videoDetailsListener.onDelete(video)
        }
        if (isAdmin) {
            holder.mDeleteVideo.visibility = View.VISIBLE
        } else {
            holder.mDeleteVideo.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mVideoTitle: TextView = mView.findViewById<View>(R.id.video_title) as TextView
        val mVideoThumbnail: ImageView = mView.findViewById<View>(R.id.video_thumbnail) as ImageView
        val mVideoDuration: TextView = mView.findViewById<View>(R.id.video_duration) as TextView
        val mVideoBy: TextView = mView.findViewById<View>(R.id.video_by) as TextView
        val mNextVideo: CardView = mView.findViewById<View>(R.id.video_cv) as CardView
        val mDeleteVideo: ImageView = mView.findViewById<View>(R.id.delete_video) as ImageView
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<VideoModel>) {
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    interface VideoDetailsListener {
        fun onClickNext(videoDetails: VideoModel)
        fun onDelete(videoDetails: VideoModel)
    }
}

