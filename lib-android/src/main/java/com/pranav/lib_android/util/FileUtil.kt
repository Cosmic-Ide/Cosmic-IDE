package com.pranav.lib_android.util

import android.content.Context

import java.io.File

class FileUtil {

  companion object {
    lateinit var mContext: Context

    @JvmStatic
    fun initializeContext(context: Context) {
      mContext = context
    }

    @JvmStatic
    fun deleteFile(path: String) {
      File(path).deleteRecursively()
    }

    @JvmStatic
    fun getDataDir(): String? {
      return mContext.getExternalFilesDir(null)?.getAbsolutePath()
    }

    @JvmStatic
    fun getJavaDir(): String {
      return getDataDir() + "/java/"
    }

    @JvmStatic
    fun getBinDir(): String {
      return getDataDir() + "/bin/"
    }

    @JvmStatic
    fun getClasspathDir(): String {
      return getDataDir() + "/classpath/"
    }
  }
}
