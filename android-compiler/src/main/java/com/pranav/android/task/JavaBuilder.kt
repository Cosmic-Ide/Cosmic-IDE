package com.pranav.android.task

import android.content.Context

import com.pranav.android.interfaces.*
import com.pranav.android.task.java.*

class JavaBuilder : Builder() {

    private var classloader: ClassLoader

    public var mContext: Context

    contructor(context: Context, loader: ClassLoader) {
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
