package com.pranav.lib_android.task.java

import com.pranav.lib_android.interfaces.*
import com.pranav.lib_android.util.FileUtil
import com.pranav.lib_android.util.ConcurrentUtil
import dalvik.system.PathClassLoader
import java.io.PrintStream
import java.io.OutputStream
import java.lang.reflect.Modifier
import java.lang.reflect.InvocationTargetException

class ExecuteJavaTask constructor(
    val mBuilder: Builder,
    val clazz: String
  ): Task() {
	
	var result: Any? = null
	val log = StringBuilder()
	
	override fun getTaskName(): String {
		return "Execute java Task"
	}

	@Throws(InvocationTargetException::class)
	override fun doFullTask() {
		val defaultOut = System.`out`
		val defaultErr = System.err
		val dexFile = FileUtil.getBinDir() + "classes.dex"
		ConcurrentUtil.execute {
			val out = object: OutputStream() {
			  override fun write(b: Int) {
			    log.append(b.toChar())
			  }
			  
			  override fun toString(): String {
			    return log.toString()
			  }
			}
			System.setOut(PrintStream(`out`))
			System.setErr(PrintStream(`out`))
			
			val loader = PathClassLoader(dexFile, mBuilder.getClassloader())
			try {

				val calledClass = loader.loadClass(clazz)
				
				val method = calledClass.getDeclaredMethod("main", Array<String>::class.java)
				
				var param = arrayOf<String>()
				
				if (Modifier.isStatic(method.getModifiers())) {
					val res = method.invoke(null, param as? Any)
					if (res !is Unit) {
					  result = res
					}
				} else if (Modifier.isPublic(method.getModifiers())) {
					val classInstance = calledClass.newInstance()
					val res = method.invoke(classInstance, param as? Any)
					if (res !is Unit) {
					  result = res
					}
				}
				if (result != null) {
				  log.append(result.toString())
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
			System.setOut(defaultOut)
			System.setErr(defaultErr)
		}
	}
	
	fun getLogs(): String {
		return log.toString()
	}
}