package com.pranav.android.task

import android.content.Context

import com.pranav.android.interfaces.*
import com.pranav.android.task.java.*

class JavaBuilder(context: Context) : Builder {

    private val mContext: Context

    init {
        mContext = context
    }

    override fun getContext() : Context {
        return mContext
    }
}
