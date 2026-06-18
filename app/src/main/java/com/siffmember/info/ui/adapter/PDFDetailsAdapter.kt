package com.siffmember.info.ui.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.siffmember.info.R
import com.siffmember.info.ui.model.PDFModel

class PDFDetailsAdapter(pdfDetails: List<PDFModel>, private val pdfDetailsListener: PDFDetailsListener, private val isAdmin: Boolean) : RecyclerView.Adapter<PDFDetailsAdapter.ViewHolder>() {

    private var filteredList = pdfDetails.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_pdf_details, parent, false)
        return ViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val pdfData = filteredList[position]
        holder.mPDFTitle.text = pdfData.pdfTitle
        holder.mPDFBy.text = "by ${pdfData.pdfBy}"

        holder.mNextPDF.setOnClickListener {
            pdfDetailsListener.onClickNext(pdfData)
        }
        holder.mDeletePDF.setOnClickListener {
            pdfDetailsListener.onDelete(pdfData)
        }
        if (isAdmin) {
            holder.mDeletePDF.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return filteredList.size
    }

    class ViewHolder(mView: View) : RecyclerView.ViewHolder(mView) {
        val mPDFTitle: TextView = mView.findViewById<View>(R.id.pdf_title) as TextView
        val mPDFBy: TextView = mView.findViewById<View>(R.id.pdf_by) as TextView
        val mNextPDF: CardView = mView.findViewById<View>(R.id.pdf_cv) as CardView
        val mDeletePDF: ImageView = mView.findViewById<View>(R.id.delete_pdf) as ImageView

    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateList(newList: List<PDFModel>) {
        filteredList.clear()
        filteredList.addAll(newList)
        notifyDataSetChanged()
    }

    interface PDFDetailsListener {
        fun onClickNext(pdfDetails: PDFModel?)
        fun onDelete(pdfDetails: PDFModel?)
    }
}

