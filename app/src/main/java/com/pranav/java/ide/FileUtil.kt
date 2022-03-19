package com.pranav.java.ide

import com.google.common.io.Files

import java.io.File

class FileUtil {

	fun deleteFile(path: String) {
		val file = File(path)

		if (!file.exists())
			return

		if (file.isFile()) {
			file.delete()
			return
		}

		val fileArr = file.listFiles()

		if (fileArr != null) {
			for (subFile : fileArr) {
				if (subFile.isDirectory()) {
					deleteFile(subFile.getAbsolutePath())
				}

				if (subFile.isFile()) {
					subFile.delete()
				}
			}
		}

		file.delete()
	}

	fun getDataDir(): String {
		return ApplicationLoader.getContext().getExternalFilesDir(null).getAbsolutePath()
	}
	
	fun getJavaDir(): String {
		return getDataDir() + "/java/"
	}
	
	fun getBinDir(): String {
		return getDataDir() + "/bin/"
	}
	
	fun getClasspathDir(): String {
		return getDataDir() + "/classpath/"
	}
}
