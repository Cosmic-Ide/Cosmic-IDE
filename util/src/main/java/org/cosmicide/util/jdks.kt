package org.cosmicide.util

import android.content.Context
import java.io.File

data class JDKInfo(
    val distributor: String,
    val version: String,
)

fun Context.jdksDir(): File = filesDir.resolve("jdks").apply { mkdirs() }

fun Context.jdks(): List<JDKInfo> {
    val jdksDir = jdksDir()

    return jdksDir.listFiles { file -> file.isDirectory }?.mapNotNull { dir ->
        val parts = dir.name.split("-", limit = 2)
        if (parts.size < 2) {
            null
        } else {
            JDKInfo(parts[0], parts[1])
        }
    } ?: emptyList()
}

fun Context.jdkNames(): List<String> = jdks().map { "${it.distributor}-${it.version}" }

/**
 * Restores executable permissions that may be lost when a JDK archive is extracted through Java
 * file streams. Gradle's launcher requires JAVA_HOME/bin/java to pass a POSIX executable check.
 */
fun repairJdkExecutablePermissions(jdkDir: File): Boolean {
    if (!jdkDir.isDirectory) return false

    jdkDir.resolve("bin").apply {
        setExecutable(true, false)
        listFiles().orEmpty()
            .filter(File::isFile)
            .forEach { it.setExecutable(true, false) }
    }

    listOf(
        jdkDir.resolve("lib/jexec"),
        jdkDir.resolve("lib/jspawnhelper")
    ).filter(File::isFile).forEach { it.setExecutable(true, false) }

    return jdkDir.resolve("bin/java").canExecute()
}
