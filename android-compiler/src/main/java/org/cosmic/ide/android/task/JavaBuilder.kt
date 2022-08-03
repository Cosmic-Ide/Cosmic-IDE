package org.cosmic.ide.android.task

import android.content.Context

import org.cosmic.ide.android.interfaces.*
import org.cosmic.ide.android.task.java.*

class JavaBuilder(context: Context) : Builder {

    private val mContext: Context

    init {
        mContext = context
    }

    override fun getContext() : Context {
        return mContext
    }
}
