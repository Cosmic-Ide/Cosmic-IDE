package org.cosmic.ide

import org.cosmic.ide.common.util.FileUtil
import java.io.File
import java.nio.file.Path
import java.util.ArrayList

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
    val platformPaths: ArrayList<Path> by lazy {
        val paths = arrayListOf<Path>()
        for (file in platformClasspath) {
            paths.add(file.toPath())
        }
        paths
    }
}
