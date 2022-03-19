package com.pranav.lib_android.task.java

import com.pranav.lib_android.FileUtil
import com.pranav.lib_android.interfaces.*
import com.pranav.ide.dx.command.dexer.Main
import java.io.File
import java.lang.reflect.Method
import java.util.ArrayList
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ThreadPoolExecutor

class DexTask: Task() {

	val mBuilder: Builder
	val ex: Exception? = null

	constructor(builder: Builder) {
		this.mBuilder = builder
	}

	override fun doFullTask() {
		val latch = CountDownLatch(1)
		Executors.newSingleThreadExecutor().execute({
			try {
				val f = File(
					FileUtil.getBinDir()
							+ "classes")
				val args = ArrayList<>()
				args.add("--debug")
				args.add("--verbose")
				args.add("--min-sdk-version")
				args.add("21")
				args.add("--output")
				args.add(f.getParent())
				args.add(f.getAbsolutePath())

				Main.clearInternTables()
				val arguments = Main.Arguments()
				val parseMethod = Main.Arguments::class.java
						.getDeclaredMethod("parse", Array<String>::class.java)
				method.isAccessible = true
				method.invoke(arguments, args.toTypedArray() as Any)
				Main.run(arguments)
			} catch (e: Exception) {
				ex = e
			}
			latch.countDown()
		})
		try {
			latch.await()
		} catch (e: InterruptedException) {
			e.printStackTrace()
		}
		if (ex != null) {
			throw ex
		}
	}

	override fun getTaskName(): String {
		return "Dex Task"
	}
}
