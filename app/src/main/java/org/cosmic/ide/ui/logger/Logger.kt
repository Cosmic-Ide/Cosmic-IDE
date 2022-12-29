package org.cosmic.ide.ui.logger

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class Logger {
    private var adapter: LogAdapter? = null
    private val data: MutableList<Log> = ArrayList()
    private var mRecyclerView: RecyclerView? = null
    fun attach(view: RecyclerView?) {
        mRecyclerView = view
        init()
    }

    private fun init() {
        adapter = LogAdapter(data)
        val layoutManager = LinearLayoutManager(
            mRecyclerView!!.context
        )
        layoutManager.stackFromEnd = true
        mRecyclerView!!.layoutManager = layoutManager
        mRecyclerView!!.adapter = adapter
    }

    fun message(message: String?) {
        mRecyclerView!!.post {
            data.add(Log(null, message!!))
            adapter!!.notifyItemInserted(data.size)
            mRecyclerView!!.smoothScrollToPosition(data.size - 1)
        }
    }
}