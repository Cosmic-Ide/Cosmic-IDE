package org.cosmicide.rewrite.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.cosmicide.project.Project
import java.io.File

/**
 * This class represents an index of files stored in a cache directory for a given project.
 */
class FileIndex(private val project: Project) {
    private val filePath by lazy { File(project.cacheDir, FILE_NAME) }

    private companion object {
        private const val FILE_NAME = "files.json"
    }

    /**
     * Adds a list of files to the index and saves it to disk.
     *
     * @param currentIndex The index of the current file in the list.
     * @param files The list of files to add to the index.
     * @throws IndexOutOfBoundsException if the current index is invalid.
     */
    fun putFiles(currentIndex: Int, files: List<File>) {
        if (files.isEmpty()) {
            return
        }

        if (currentIndex < 0 || currentIndex >= files.size) {
            throw IndexOutOfBoundsException("Invalid current index: $currentIndex")
        }

        val filePaths = files.distinctBy { it.absolutePath }
            .toMutableList()
            .apply { add(0, removeAt(currentIndex)) }

        if (project.cacheDir.exists().not()) {
            project.cacheDir.mkdir()
        }

        val json = Gson().toJson(filePaths)

        filePath.writeText(json)
    }

    /**
     * Gets a list of files from the index.
     *
     * @return A list of files from the index.
     */
    fun getFiles(): List<File> {
        if (filePath.exists().not()) {
            filePath.parentFile?.mkdirs()
            filePath.bufferedWriter().use { it.write("[]") }
            return emptyList()
        }

        val json = filePath.readText()
        val filePaths = Gson().fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)

        if (filePaths.isNullOrEmpty()) {
            return emptyList()
        }

        return filePaths.map { File(it) }
            .filter(File::exists)
            .distinctBy { it.absolutePath }
    }
}