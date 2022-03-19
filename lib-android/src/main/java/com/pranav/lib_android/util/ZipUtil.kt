package com.pranav.ide.util

import android.content.Context
import android.util.Log

import com.google.common.io.ByteStreams
import com.google.common.io.Files

import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ZipUtil {
	val BUFFER_SIZE: Int = 1024 * 10
	val TAG: String = "ZipUtil"
	
	fun unzipFromAssets(context: Context, zipFile: String,
	destination: String) {
		try {
			if (destination == null || destination.length() == 0)
			destination = context.getFilesDir().getAbsolutePath()
			var stream: InputStream = context.getAssets().open(zipFile)
			unzip(stream, destination)
		} catch (e: IOException) {
			e.printStackTrace()
		}
	}
	
	fun unzip(stream: InputStream, destination: String) {
		dirChecker(destination, "")
		var buffer = new byte[BUFFER_SIZE]
		try {
			ZipInputStream zin = new ZipInputStream(stream)
			var ze: ZipEntry = null
			
			while ((ze = zin.getNextEntry()) != null) {
				Log.v(TAG, "Unzipping " + ze.getName())
				
				if (ze.isDirectory()) {
					dirChecker(destination, ze.getName())
				} else {
					val f = File(destination, ze.getName())
					if (!f.exists()) {
						val success = f.createNewFile()
						if (!success) {
							Log.w(TAG, "Failed to create file " + f.getName())
							continue
						}
						val fout = FileOutputStream(f)
						var count: Int
						while ((count = zin.read(buffer)) != -1) {
							fout.write(buffer, 0, count)
						}
						zin.closeEntry()
						fout.close()
					}
				}
			}
			zin.close()
		} catch (e: Exception) {
			Log.e(TAG, "unzip", e)
		}
	}
	
	fun dirChecker(destination: String, dir: String) {
		val f = File(destination, dir)
		
		if (!f.isDirectory()) {
			val success = f.mkdirs()
			if (!success) {
				Log.w(TAG, "Failed to create folder " + f.getName())
			}
		}
	}
	
	
	fun copyFileFromAssets(context: Context, inputFile: String, fileName: String) {
		val inp = context.getAssets().open(inputFile)
		val outputPath = context.getFilesDir() + "/" + fileName
		val outp = new File(outputPath)
		Files.write(ByteStreams.toByteArray(inp), outp)
	}
}
