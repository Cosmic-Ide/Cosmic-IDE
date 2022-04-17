package com.pranav.lib_android.util

import android.content.Context
import android.util.Log

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipUtil {
	private val BUFFER_SIZE = 1024 * 10
	private val TAG = "ZipUtil"

	fun unzipFromAssets(context: Context, zipFile: String, destination: String) {
		try {
			val stream = context.getAssets().`open`(zipFile)
			unzip(stream, destination)
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}

	private fun unzip(stream: InputStream, destination: String) {
		dirChecker(destination, "")
		val buffer = Byte[BUFFER_SIZE]
		try {
			val zin = ZipInputStream(stream)
			var ze: ZipEntry = null

			while ((var ze = zin.getNextEntry()) != null) {
				if (ze.isDirectory()) {
					dirChecker(destination, ze.getName())
				} else {
					val f = File(destination, ze.getName())
					if (!f.normalize().startsWith(destination))
						throw SecurityException("Potentially harmful files detected inside zip")
					if (!f.exists()) {
						val success = f.createNewFile()
						if (!success) {
							Log.w(TAG, "Failed to create file " + f.getName())
							continue
						}
						f.appendBytes(zin.readBytes())
						zin.closeEntry()
						fout.close()
					}
				}
			}
			zin.close()
		} catch (e: IOException) {
			Log.e(TAG, "unzip", e)
		}
	}

	private fun dirChecker(destination: String, dir: String) {
		val f = File(destination, dir)

		if (!f.isDirectory() && !f.mkdirs()) {
				Log.w(TAG, "Failed to create folder " + f.getName())
		}
	}
}