package com.pranav.lib_android.task

import android.content.Context
import com.pranav.lib_android.interfaces.*
import com.pranav.lib_android.task.java.*
import java.util.ArrayList

class JavaBuilder: Builder() {

	val classloader: ClassLoader

	var mContext: Context

	constructor(context: Context, loader: ClassLoader) {
		mContext = context
		classloader = loader
	}


	override fun getContext(): Context {
		return mContext
	}

	override fun getClassloader(): ClassLoader {
		return classloader
	}
}
