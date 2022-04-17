package com.pranav.lib_android.task

import android.content.Context
import com.pranav.lib_android.interfaces.Builder

class JavaBuilder(context: Context, loader: ClassLoader): Builder() {

	var classloader: ClassLoader

	var mContext: Context

	init {
		mContext = context
		classloader = loader
	}

	override fun getContext() = mContext

	override fun getClassloader() = classloader
}
