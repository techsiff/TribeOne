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
import com.siffmember.info.R
import com.siffmember.info.ui.model.CategoryList

class VideoFolderDetailsAdapter(private val mContext: Context, videoDetails: List<CategoryList>, private val videoDetailsListener: VideoDetailsListener, private val isAdmin: Boolean) : RecyclerView.Adapter<VideoFolderDetailsAdapter.ViewHolder>() {

    private var filteredList = videoDetails.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_folder_details, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = filteredList[position]
        holder.mVideoTitle.text = video.name
        holder.mVideoBy.text = "by ${video.createdBy}"
        holder.mVideoFolder.visibility = View.VISIBLE
        holder.mNextVideo.setOnClickListener {
            videoDetailsListener.onClickNext(video)
        }
        holder.mDeleteVideo.visibility = View.GONE
        holder.mDeleteVideo.setOnClickListener {
            videoDetailsListener.onDelete(video)
        }
        if (isAdmin) {
            holder.mDeleteVideo.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mVideoTitle: TextView = mView.findViewById<View>(R.id.video_title) as TextView
        val mVideoBy: TextView = mView.findViewById<View>(R.id.video_by) as TextView
        val mNextVideo: CardView = mView.findViewById<View>(R.id.video_cv) as CardView
        val mDeleteVideo: ImageView = mView.findViewById<View>(R.id.delete_video) as ImageView
        val mVideoFolder: ImageView = mView.findViewById<View>(R.id.video_folder) as ImageView
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<CategoryList>) {
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    interface VideoDetailsListener {
        fun onClickNext(videoDetails: CategoryList)
        fun onDelete(videoDetails: CategoryList)
    }
}

