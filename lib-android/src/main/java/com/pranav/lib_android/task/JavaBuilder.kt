package com.pranav.lib_android.task

import android.content.Context
import com.pranav.lib_android.interfaces.*
import com.pranav.lib_android.task.java.*
import java.util.ArrayList

class JavaBuilder: Builder() {

	val classloader: ClassLoader

	lateinit val mContext: Context

	constructor(context: Context, loader: ClassLoader) {
		mContext = context
		classloader = loader
	}

	override fun getTasks(): Task[] {
		val tasks = ArrayList<>()
		tasks.add(CompileJavaTask(this))
		tasks.add(DexTask(this))
		// tasks.add(new ExecuteDexTask())
		return tasks.toArray(new Task[0])
	}

	@Override
	Context getContext() {
		return mContext
	}

	override fun getClassloader(): ClassLoader {
		return this.classloader
	}
}
