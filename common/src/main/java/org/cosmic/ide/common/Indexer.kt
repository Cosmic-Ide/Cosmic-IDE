package org.cosmic.ide.common

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.cosmic.ide.common.util.FileUtil
import org.json.JSONException
import java.io.File
import java.io.IOException

class Indexer @Throws(JSONException::class) constructor(projectCachePath: String) {

    private lateinit var json: String

    private var filePath: String

    init {
        filePath = projectCachePath + "lastOpenedFiles.json"
        load()
    }

    @Throws(JSONException::class)
    fun load() {
        val indexFile = File(filePath)
        try {
            if (!indexFile.exists()) {
                FileUtil.writeFile(filePath, "{}")
            }
            val index = FileUtil.readFile(indexFile)
            json = index
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(JSONException::class)
    fun putPathOpenFiles(items: List<File>): Indexer {
        val filesPath = mutableListOf<String>()
        items.forEach {
            filesPath.add(it.absolutePath)
        }
        val value = Gson().toJson(filesPath)
        json = value
        return this
    }

    fun getPathOpenFiles(): List<File> {
        return try {
            val type = object : TypeToken<List<String>>() {}.type
            val filesPath: List<String> = Gson().fromJson(getString(), type)
            val files = mutableListOf<File>()
            filesPath.forEach {
                files.add(File(it))
            }
            files
        } catch (ignored: Exception) {
            mutableListOf()
        }
    }

    @Throws(JSONException::class)
    fun put(value: Long): Indexer {
        json = value.toString()
        return this
    }

    @Throws(JSONException::class)
    fun getString(): String {
        return json
    }

    fun getLong(): Long {
        return try {
            json.toLong()
        } catch (e: JSONException) {
            0
        }
    }

    fun flush() {
        try {
            FileUtil.writeFile(filePath, getString())
        } catch (ignore: Throwable) {
        }
    }
}
