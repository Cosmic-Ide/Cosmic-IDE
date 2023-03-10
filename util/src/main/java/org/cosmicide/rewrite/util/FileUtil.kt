package org.cosmicide.rewrite.util

import android.content.Context
import java.io.File

class FileUtil {

    companion object {
        lateinit var projectDir: File
        lateinit var classpathDir: File
        lateinit var dataDir: File
        fun init(context: Context) {
            dataDir = context.filesDir
            projectDir = File(dataDir, "projects")
            classpathDir = File(dataDir, "classpath")
            projectDir.mkdirs()
            classpathDir.mkdirs()
        }
    }
}