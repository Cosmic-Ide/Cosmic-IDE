package org.cosmic.ide.android.task

import android.content.Context
import org.cosmic.ide.android.interfaces.Builder

class JavaBuilder(context: Context) : Builder {

    private val mContext: Context

    init {
        mContext = context
    }

    override fun getContext(): Context {
        return mContext
    }
}
