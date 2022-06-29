package com.pranav.android.task

import android.content.Context

import com.pranav.android.interfaces.*
import com.pranav.android.task.java.*

class JavaBuilder(context: Context, loader: ClassLoader) : Builder {

    private var classloader: ClassLoader

    private var mContext: Context

    init {
        mContext = context
        classloader = loader
    }

    override fun getContext() : Context {
        return mContext
    }

    override fun getClassloader() : ClassLoader {
        return classloader
    }
}
