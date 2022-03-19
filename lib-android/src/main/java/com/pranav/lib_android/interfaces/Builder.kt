package com.pranav.lib_android.interfaces

import android.content.Context

abstract class Builder {

	abstract fun getContext(): Context
	abstract fun getClassloader(): ClassLoader
}
