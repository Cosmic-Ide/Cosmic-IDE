package com.pranav.lib_android.task.java

import com.pranav.lib_android.interfaces.*
import com.pranav.lib_android.util.BinaryExecutor
import com.pranav.lib_android.util.ZipUtil
import com.pranav.lib_android.FileUtil
import com.pranav.lib_android.exception.CompilationFailedException

import java.io.File
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch

class ZipAlignTask: Task {

	val mBuilder: Builder
	val executor: BinaryExecutor
	val ex = null

	constructor ZipAlignTask(builder: Builder) {
		this.mBuilder = builder
		this.executor = BinaryExecutor()
	}

	override fun doFullTask() {
		Executors.newSingleThreadExecutor().execute(() -> {
			try {
				val zipalign = mBuilder.getContext().getFilesDir()
						+ "/arm64-v8a"
				if (!File(zipalign).exists()) {
					ZipUtil.copyFileFromAssets(mBuilder.getContext(),
							"zipalign/arm64-v8a", "arm64-v8a")
				}
				Runtime.getRuntime().exec("chmod 777 " + zipalign)
				val args = ArrayList<>()
				args.add(zipalign)
				args.add("4")
				args.add(FileUtil.getBinDir()
						+ "classes.jar")
				args.add(FileUtil.getBinDir()
						+ "zipAligned.jar")
				executor.execute(args)
			} catch (e: Throwable) {
				ex = e
			}
		})
		if (executor.getLogs() != "") {
			throw CompilationFailedException(executor.getLogs())
		}
		if (ex != null)
			throw ex
	}

	override fun getTaskName(): String {
		return "Zip Align Task"
	}
}
