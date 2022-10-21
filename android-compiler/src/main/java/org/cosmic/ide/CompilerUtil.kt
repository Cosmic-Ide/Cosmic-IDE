package org.cosmic.ide

import org.cosmic.ide.common.util.FileUtil

import java.util.ArrayList
import java.nio.file.Path
import java.nio.file.Paths
import java.io.File

object CompilerUtil {
    @JvmStatic
    val platformClasspath: ArrayList<File> by lazy {
        arrayListOf(
                File(FileUtil.getClasspathDir(), "android.jar"),
                File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar"),
                File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.7.20.jar"),
                File(FileUtil.getClasspathDir(), "kotlin-stdlib-common-1.7.20.jar")
        )
    }

    @JvmStatic
    fun getPlatformPaths(): ArrayList<Path> {
       val paths = arrayListOf<Path>()
       for (file in platformClasspath) {
           paths.add(file.toPath())
       }
       return paths
   }
}