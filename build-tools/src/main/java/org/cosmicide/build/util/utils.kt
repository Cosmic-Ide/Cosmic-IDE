package org.cosmicide.build.util

import org.cosmicide.rewrite.util.FileUtil
import java.io.File

fun getSourceFiles(dir: File, extension: String): List<File> {
    val files = mutableListOf<File>()
    for (file in dir.listFiles()!!) {
        if (file.isDirectory) {
            files.addAll(getSourceFiles(file, extension))
        } else if (file.extension == extension) {
            files.add(file)
        }
    }
    return files
}

fun getSystemClasspath(): List<File> {
    val classpath = mutableListOf<File>()
    FileUtil.classpathDir.listFiles()?.forEach { classpath.add(it) }
    return classpath
}
