package com.pranav.android.interfaces

import android.content.Context

abstract class Builder {

    abstract fun getContext() : Context

    abstract fun getClassloader() : ClassLoader
}
