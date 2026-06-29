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
        val parts = dir.name.split("-", limit = 3)
        if (parts.size < 2) {
            null
        } else {
            JDKInfo(parts[1], parts[2])
        }
    } ?: emptyList()
}

fun Context.jdkNames(): List<String> = jdks().map { "${it.distributor}-${it.version}" }
