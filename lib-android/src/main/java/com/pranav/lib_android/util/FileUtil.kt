package com.pranav.lib_android.util

import android.content.Context

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException

class FileUtil {
    companion object {
    private lateinit var mContext: Context

    @JvmStatic
    fun initializeContext(context: Context) {
        mContext = context
    }

    @JvmStatic
    fun createDirectory(path: String) = File(path).mkdir()

    @JvmStatic
    fun writeFile(path: String, content: String) {
        val file = File(path)
        file.getParentFile().mkdirs()
        if (!file.exists()) file.createNewFile()
        file.writeText(content)
    }

    @JvmStatic
    fun writeBytes(path: String, bytes: ByteArray) {
        File(path).writeBytes(bytes)
    }

    @JvmStatic
    fun readFile(file: File) = file.readText()

    @JvmStatic
    fun asByteArray(in: InputStream) = in.readBytes()

    @JvmStatic
    fun deleteFile(path: String) {
        File(path).deleteRecursively()
    }

    @JvmStatic
    fun getFileName(path: String): String {
        val splited = path.split("/")
        return splited[splited.length - 1]
    }

    @JvmStatic
    private fun getDataDir() = mContext.getFilesDir()

    @JvmStatic
    fun getJavaDir() = getDataDir() + "/java/"

    @JvmStatic
    fun getBinDir() = getDataDir() + "/bin/"

    @JvmStatic
    fun getCacheDir() = getDataDir() + "/cache/"

    @JvmStatic
    fun getClasspathDir() = getDataDir() + "/classpath/"
}
