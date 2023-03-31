package org.cosmicide.build.util

import org.cosmicide.rewrite.util.FileUtil
import java.io.File

/**
 * Returns a list of files with the given extension within the specified directory and its subdirectories.
 *
 * @param directory The directory to search for files.
 * @param extension The file extension to filter by.
 * @return A list of files with the given extension within the specified directory and its subdirectories.
 */
fun getSourceFiles(directory: File, extension: String): List<File> {
    return directory.listFiles()
        ?.flatMap { if (it.isDirectory) getSourceFiles(it, extension) else listOf(it) }
        ?.filter { it.extension == extension }
        ?: emptyList()
}

/**
 * Returns a list of files in the system classpath.
 *
 * @return A list of files in the system classpath.
 */
fun getSystemClasspath(): List<File> {
    return FileUtil.classpathDir.listFiles()?.toList() ?: emptyList()
}