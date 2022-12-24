package org.cosmic.ide.common.util

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

object FileUtil {

    private lateinit var privateDataDirectory: String

    @JvmStatic
    fun setDataDirectory(directory: String) {
        privateDataDirectory = directory + "/"
    }

    @JvmStatic
    fun createDirectory(path: String): Boolean {
        return File(path).mkdir()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeFile(inp: InputStream, path: String) {
        val filePath = Paths.get(path).normalize()
        Files.copy(inp, filePath, StandardCopyOption.REPLACE_EXISTING)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeFile(file: String, content: String) {
        File(file).writeText(content)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeFile(path: String, content: ByteArray) {
        File(path).writeBytes(content)
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readFile(file: File): String {
        return file.readText()
    }

    @JvmStatic
    fun deleteFile(path: String) {
        try {
            val file = File(path)
            if (file.isFile) {
                file.delete()
                return
            }

            file.deleteRecursively()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @JvmStatic
    fun getDataDir() = privateDataDirectory

    @JvmStatic
    fun getProjectsDir() = getDataDir() + "projects/"

    @JvmStatic
    fun getClasspathDir() = getDataDir() + "classpath/"

    @JvmStatic
    fun createOrExistsDir(dirPath: String) = createOrExistsDir(File(dirPath))

    @JvmStatic
    fun createOrExistsDir(file: File) = if (file.exists()) file.isDirectory else file.mkdirs()
}
