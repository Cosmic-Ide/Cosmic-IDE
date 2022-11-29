package org.cosmic.ide.ui.logger

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class LogAdapter(val mData: List<Log>) :
    RecyclerView.Adapter<LogAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(FrameLayout(parent.context))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val log = mData[position]

        holder.textView.text = log.message
    }

    override fun getItemCount() = mData.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        lateinit var textView: TextView

        init {
            textView = TextView(itemView.context)
            (itemView as ViewGroup).addView(textView)
        }
    }
}

data class Log(val message: String)