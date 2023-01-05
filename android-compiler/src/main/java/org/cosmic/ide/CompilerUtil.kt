package org.cosmic.ide

import org.cosmic.ide.common.util.FileUtil
import java.io.File
import java.nio.file.Path

object CompilerUtil {
    @JvmStatic
    val platformClasspath: List<File> by lazy {
        listOf(
            File(FileUtil.getClasspathDir(), "android.jar"),
            File(FileUtil.getClasspathDir(), "core-lambda-stubs.jar"),
            File(FileUtil.getClasspathDir(), "kotlin-stdlib-1.8.0.jar"),
            File(FileUtil.getClasspathDir(), "kotlin-stdlib-common-1.8.0.jar")
        )
    }

    @JvmStatic
    val platformPaths: List<Path> by lazy {
        val paths = mutableListOf<Path>()
        for (file in platformClasspath) {
            paths.add(file.toPath())
        }
        paths
    }
}
