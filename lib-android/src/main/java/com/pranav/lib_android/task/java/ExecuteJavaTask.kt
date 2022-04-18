package com.pranav.lib_android.task.java

import com.pranav.lib_android.interfaces.*
import com.pranav.lib_android.util.FileUtil
import com.pranav.lib_android.util.ConcurrentUtil
import dalvik.system.PathClassLoader
import java.io.PrintStream
import java.io.OutputStream
import java.lang.reflect.Modifier

class ExecuteJavaTask constructor(
    val mBuilder: Builder,
    val clazz: String
  ): Task() {
	
	var result: Any?
	val log = StringBuilder()
	
	override fun getTaskName(): String {
		return "Execute java Task"
	}
	
	override fun doFullTask() {
		val defaultOut = System.`out`
		val defaultErr = System.err
		val dexFile = FileUtil.getBinDir() + "classes.dex"
		ConcurrentUtil.execute({
			val out = object: OutputStream() {
			  override fun write(b: Int) {
			    log.append(b)
			  }
			  
			  override fun toString(): String {
			    return log.toString()
			  }
			}
			System.setOut(PrintStream(out))
			System.setErr(PrintStream(out))
			
			val loader = PathClassLoader(dexFile, mBuilder.getClassloader())
			try {

				val calledClass = loader.loadClass(clazz)
				
				val method = calledClass.getDeclaredMethod("main", Array<String>::class.java)
				
				var param = arrayOf<String>()
				
				if (Modifier.isStatic(method.getModifiers())) {
					result = method.invoke(null, param as? Object)
				} else if (Modifier.isPublic(method.getModifiers())) {
					val classInstance = calledClass.newInstance()
					result = method.invoke(classInstance, param as? Object)
				}
				if (result != null) {
				  System.out.println(result.toString())
				}
			} catch (e: Exception) {
				e.printStackTrace()
			}
			System.setOut(defaultOut)
			System.setErr(defaultErr)
		})
	}
	
	fun getLogs(): String {
		return log.toString()
	}
}