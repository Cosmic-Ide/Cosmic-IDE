package com.pranav.lib_android.util

import android.content.Context

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStream

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
    @Throws(IOException:class)
    fun writeFile(path: String, content: String) {
        val file = File(path)
        file.getParentFile().mkdirs()
        if (!file.exists()) file.createNewFile()
        file.writeText(content)
    }

    @JvmStatic
    @Throws(IOException:class)
    fun writeBytes(path: String, bytes: ByteArray) {
        File(path).writeBytes(bytes)
    }

    @JvmStatic
    @Throws(IOException:class)
    fun readFile(file: File) = file.readText()

    @JvmStatic
    @Throws(IOException:class)
    fun asByteArray(inp: InputStream) = inp.readBytes()

    @JvmStatic
    fun deleteFile(path: String) {
        File(path).deleteRecursively()
    }

    @JvmStatic
    fun getFileName(path: String): String {
        val splited = path.split("/")
        return splited[splited.size - 1]
    }

    @JvmStatic
    private fun getDataDir(): String {
        return mContext.getFilesDir().getAbsolutePath()
    }

    @JvmStatic
    fun getJavaDir() = getDataDir() + "/java/"

    @JvmStatic
    fun getBinDir() = getDataDir() + "/bin/"

    @JvmStatic
    fun getCacheDir() = getDataDir() + "/cache/"

    @JvmStatic
    fun getClasspathDir() = getDataDir() + "/classpath/"
    }
}
