package com.pranav.lib_android.task.java

import android.net.Uri
import com.pranav.java.ide.FileUtil
import com.pranav.lib_android.interfaces.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.jar.Attributes
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

class JarTask: Task {
	val mBuilder: Builder

	constructor(builder: Builder) {
		this.mBuilder = builder
	}

	override fun doFullTask() {

		//input file
		val classesFolder = File(FileUtil.getBinDir() + "classes")
		
		// Open archive file
		val stream = FileOutputStream(File(FileUtil.getBinDir() + "classes.jar"))
		
		val manifest = buildManifest()
		
		//Create the jar file
		val out = JarOutputStream(stream, manifest)
		
		//Add the files..
		if (classesFolder.listFiles() != null) {
			for (File clazz : classesFolder.listFiles()) {
				add(classesFolder.getPath(), clazz, out)
			}
		}
		
		out.close()
		stream.close()
	}

	override fun getTaskName(): String {
		return "JarTask"
	}

	fun buildManifest(): Manifest {
		val manifest = Manifest()
		manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0")
		return manifest
	}
	
	fun add(parentPath: String, source: File, target: JarOutputStream) {
		val name = source.getPath().substring(parentPath.length() + 1)
		
		var in = null
		try {
			if (source.isDirectory()) {
				if (!name.isEmpty()) {
					if (!name.endsWith("/"))
					    name += "/"
					
					//Add the Entry
					val entry = JarEntry(name)
					entry.setTime(source.lastModified())
					target.putNextEntry(entry)
					target.closeEntry()
				}
				
				for (File nestedFile : source.listFiles()) {
					add(parentPath, nestedFile, target)
				}
				return
			}
			
			val entry = JarEntry(name)
			entry.setTime(source.lastModified())
			target.putNextEntry(entry)
			in = BufferedInputStream(FileInputStream(source))
			byte[] buffer = 
			byte[1024]
			while (true) {
				int count = in.read(buffer)
				if (count == -1)
				break
				target.write(buffer, 0, count)
			}
			target.closeEntry()
			
		} finally {
			if (in != null) {
				in.close()
			}
		}
	}
	
	fun getDefAttrs(): Attributes {
		val attrs = Attributes()
		attrs.put(Attributes.Name("Created-By"), "heystudios")
		
		return attrs
	}
}
