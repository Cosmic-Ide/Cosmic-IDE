package org.cosmic.ide.common.util

import java.io.BufferedWriter
import java.io.File
import java.io.FileFilter
import java.io.FileWriter
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.BasicFileAttributes

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
        val path = Paths.get(file);
        Files.createDirectories(path.parent)
        Files.write(path, content.toByteArray())
    }

    @JvmStatic
    @Throws(IOException::class)
    fun writeFile(file: String, content: ByteArray) {
        val path = Paths.get(file)
        Files.createDirectories(path.parent)
        Files.write(path, content);
    }

    @JvmStatic
    @Throws(IOException::class)
    fun readFile(file: File): String {
        return String(Files.readAllBytes(file.toPath()))
    }

    @JvmStatic
    fun deleteFile(p: String) {
        try {
            val path = Paths.get(p)
            if (Files.isRegularFile(path)) {
                Files.delete(path)
                return
            }

            File(p).deleteRecursively()
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
    fun createOrExistsDir(dirPath: String) = createOrExistsDir(getFileByPath(dirPath))

    @JvmStatic
    fun createOrExistsDir(file: File?) = if (file?.exists()) file?.isDirectory() else file?.mkdirs()

    @JvmStatic
    fun getFileByPath(filePath: String): File? {
        return if (isSpace(filePath)) null else File(filePath)
    }

    private fun isSpace(s: String): Boolean {
        if (s == null) {
            return true
        }
        for (c in s.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                return false
            }
        }
        return true
    }
}
