package com.pranav.android.interfaces

import android.content.Context

interface Builder {

    fun getContext() : Context

    fun getClassloader() : ClassLoader
}
