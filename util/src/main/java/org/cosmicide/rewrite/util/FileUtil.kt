package org.cosmicide.rewrite.util

import android.content.Context
import java.io.File

object FileUtil {

    lateinit var projectDir: File
    lateinit var classpathDir: File
    lateinit var dataDir: File
    lateinit var pluginDir: File

    fun init(context: Context) {
        dataDir = context.getExternalFilesDir(null)!!
        projectDir = File(dataDir, "projects").apply { mkdirs() }
        classpathDir = File(dataDir, "classpath").apply { mkdirs() }
        pluginDir = File(dataDir, "plugins").apply { mkdirs() }
    }
}