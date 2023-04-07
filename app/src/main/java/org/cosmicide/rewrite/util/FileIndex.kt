package org.cosmicide.rewrite.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.cosmicide.project.Project
import java.io.File
import java.io.IOException

class FileIndex(project: Project) {

    private val path = File(project.cacheDir, "files.json")

    fun putFiles(current: Int, files: List<File>) {
        if (files.isEmpty()) return
        val items = files.toMutableList()
        val filesPaths = items.map { it.absolutePath }.toMutableList()
        filesPaths.add(0, filesPaths.removeAt(current))
        try {
            path.parentFile?.mkdirs()
            path.createNewFile()
            path.writeText(Gson().toJson(filesPaths))
        } catch (e: IOException) {
            // handle exception
        }
    }

    fun getFiles(): List<File> {
        if (!path.exists()) return emptyList()
        val value = path.readText()
        return Gson().fromJson<List<String>>(value, object : TypeToken<List<String>>() {}.type)
            .map { File(it) }
    }
}