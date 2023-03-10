package org.cosmicide.rewrite.util

import com.google.gson.Gson
import org.cosmicide.project.Project
import java.io.File

class FileIndex(project: Project) {

    private val path = File(project.cacheDir.absolutePath + File.pathSeparator + "files.json")

    fun putFiles(current: Int, files: List<File>) {
        if (files.isEmpty()) return
        val items = files.toMutableList()
        val filesPath = mutableListOf<String>()
        filesPath.add(items[current].absolutePath)
        items.removeAt(current)
        items.forEach {
            filesPath.add(it.absolutePath)
        }
        val value = Gson().toJson(filesPath)
        path.parentFile?.mkdirs()
        path.createNewFile()
        path.writeText(value)
    }

    fun getFiles(): List<File> {
        if (!path.exists()) return emptyList()
        val value = path.readText()
        return (Gson().fromJson(value, List::class.java) as List<String>).map { File(it) }
    }
}
