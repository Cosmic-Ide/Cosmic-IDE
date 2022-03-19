package com.pranav.lib_android.task.java

import com.pranav.lib_android.interfaces.*
import com.pranav.lib_android.FileUtil
import dalvik.system.PathClassLoader
import java.io.*
import java.security.Permission
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.concurrent.Executors
import java.util.concurrent.CountDownLatch

class ExecuteJavaTask: Task() {
	
	lateinit val mBuilder: Builder
	var clazz: String
	var log = StringBuilder()
	
	constructor(builder: Builder, claz: String) {
		this.mBuilder = builder
		this.clazz = claz
	}
	
	override fun getTaskName(): String {
		return "Execute java Task"
	}
	
	override fun doFullTask() {
		val defaultOut = System.out
		val defaultErr = System.err
		val dexFile = FileUtil.getBinDir()
		+ "classes.dex"
		final CountDownLatch latch = CountDownLatch(1)
		Executors.newSingleThreadExecutor().execute({
				val out = OutputStream() {
					override fun write(b: Int) {
						log.append(String.valueOf((Char) b))
					}
					
					override fun toString(): String {
						return log.toString()
					}
				}
				System.setOut(PrintStream(out))
				System.setErr(PrintStream(out))
				
				val loader = PathClassLoader(dexFile,
				mBuilder.getClassloader())
				try {

					val calledClass = loader.loadClass(clazz)
					
					val method = calledClass.getDeclaredMethod("main", Array<String>::class.java)
					
					var param: String[] = {}
					var result: Object
					if (Modifier.isStatic(method.getModifiers())) {
						result = method.invoke(null, Object[] {param})
					} else if (Modifier.isPublic(method.getModifiers())) {
						Object classInstance = calledClass.newInstance()
						result = method.invoke(classInstance, Object[] {param})
					}
				} catch (e: Exception) {
					e.printStackTrace()
				}
				System.setSecurityManager(null)
				latch.countDown()
			}
		})
		try {
			latch.await()
		} catch (ignored: InterruptedException) {
		}
		System.setOut(defaultOut)
		System.setErr(defaultErr)
	}
	
	fun getLogs(): String {
		return log.toString()
	}
}
