package com.pranav.lib_android.interfaces

import android.content.Context

open class Builder {

	open fun getContext(): Context

	open fun getClassloader(): ClassLoader
}
