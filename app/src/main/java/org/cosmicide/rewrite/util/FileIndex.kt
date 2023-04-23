package org.cosmicide.rewrite.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.cosmicide.project.Project
import java.io.File

class FileIndex(private val project: Project) {
    private val filePath = File(project.cacheDir, "files.json")

    /**
     * Adds a list of files to the index and saves it to disk.
     *
     * @param currentIndex The index of the current file in the list.
     * @param files The list of files to add to the index.
     */
    fun putFiles(currentIndex: Int, files: List<File>) {
        if (files.isEmpty()) return

        if (currentIndex < 0 || currentIndex >= files.size) {
            throw IndexOutOfBoundsException("Invalid current index: $currentIndex")
        }

        val uniqueFiles = files.distinctBy { it.absolutePath }
        val filePaths = uniqueFiles.map { it.absolutePath }.toMutableList()
        filePaths.add(0, filePaths.removeAt(currentIndex))
        val cacheDir = project.cacheDir
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val json = Gson().toJson(filePaths)
        filePath.writeText(json)
    }

    /**
     * @return A list of files from the index.
     */
    fun getFiles(): List<File> {
        if (!filePath.exists()) {
            filePath.parentFile!!.mkdirs()
            filePath.createNewFile()
            return emptyList()
        }

        val json = filePath.readText()
        val filePaths =
            Gson().fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
        if (filePaths.isNullOrEmpty()) return emptyList()
        return filePaths
            .map { File(it) }
            .distinctBy { it.absolutePath }
            .filter(File::exists)
    }
}