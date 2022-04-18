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
  companion object {
    private val TAG = "ZipUtil"

    @JvmStatic
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
      try {
        val zin = ZipInputStream(stream)
        var ze: ZipEntry? = zin.getNextEntry()

        label@ while (ze != null) {
          if (ze.isDirectory()) {
            dirChecker(destination, ze.getName())
          } else {
            val f = File(destination, ze.getName())
            if (!f.normalize().startsWith(destination))
            throw SecurityException("Potentially harmful files detected inside zip")
            if (!f.exists()) {
              val success = f.createNewFile()
              if (!success) continue@label
              f.appendBytes(zin.readBytes())
              zin.closeEntry()
            }
          }
          try {
            ze = zin.getNextEntry()
          } catch (e: NullPointerException) {
            ze = null
          }
        }
        zin.close()
      } catch (e: IOException) {
        Log.e(TAG, "Unzip", e)
      }
    }

    private fun dirChecker(destination: String, dir: String) {
      val f = File(destination, dir)

      if (!f.isDirectory() && !f.mkdirs()) {
        Log.w(TAG, "Failed to create folder " + f.getName())
      }
    }
	}
}