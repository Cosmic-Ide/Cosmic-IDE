package org.cosmicide.util

import android.system.Os
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun InputStream.unzip(
    targetDir: File,
    stripPrefix: String = "",
    skipExisting: Boolean = false,
    makeReadable: Boolean = false,
    symlinkManifest: String = ".symlinks"
) {
    val normalizedTargetDir = targetDir.normalize()
    val normalizedStripPrefix = stripPrefix
        .replace('\\', '/')
        .trimStart('/')
        .trimEnd('/')
        .let { if (it.isEmpty()) "" else "$it/" }

    val symlinks = mutableListOf<Pair<String, String>>()

    targetDir.mkdirs()

    fun cleanEntryName(name: String): String? {
        val cleanName = name
            .replace('\\', '/')
            .trimStart('/')

        if (cleanName.isBlank()) return null
        if (cleanName.indexOf('\u0000') != -1) return null

        val relativeName = if (normalizedStripPrefix.isNotEmpty()) {
            when {
                cleanName == normalizedStripPrefix.trimEnd('/') -> return null
                cleanName.startsWith(normalizedStripPrefix) -> cleanName.removePrefix(
                    normalizedStripPrefix
                )

                else -> return null
            }
        } else {
            cleanName
        }

        if (relativeName.isBlank()) return null

        val parts = relativeName.split('/')
        if (parts.any { it.isBlank() || it == "." || it == ".." }) return null

        return relativeName
    }

    ZipInputStream(this).use { zipIn ->
        while (true) {
            val ze = zipIn.nextEntry ?: break

            try {
                val relativeName = cleanEntryName(ze.name)
                    ?: continue

                if (!ze.isDirectory && relativeName == symlinkManifest) {
                    val manifestText = zipIn
                        .readBytes()
                        .toString(Charsets.UTF_8)

                    manifestText
                        .lineSequence()
                        .forEach { line ->
                            val tab = line.indexOf('\t')
                            if (tab <= 0) return@forEach

                            val linkPath = line.substring(0, tab).trim()
                            val linkTarget = line.substring(tab + 1).trim()

                            if (linkPath.isNotBlank() && linkTarget.isNotBlank()) {
                                symlinks += linkPath to linkTarget
                            }
                        }

                    continue
                }

                val resolved = normalizedTargetDir.resolve(relativeName).normalize()

                if (!resolved.startsWith(normalizedTargetDir)) {
                    throw SecurityException("Entry with an illegal path: ${ze.name}")
                }

                if (ze.isDirectory) {
                    resolved.mkdirs()
                    continue
                }

                resolved.parentFile?.mkdirs()

                if (!skipExisting || !resolved.exists()) {
                    resolved.outputStream().use { output ->
                        zipIn.copyTo(output)
                    }
                }

                if (makeReadable && resolved.exists()) {
                    resolved.setReadable(true, false)
                }
            } finally {
                try {
                    zipIn.closeEntry()
                } catch (_: Exception) {
                    // If a malformed entry or platform quirk already closed it,
                    // do not crash extraction cleanup.
                }
            }
        }
    }

    symlinks.forEach { (relativeLinkPath, rawTarget) ->
        val linkName = relativeLinkPath
            .replace('\\', '/')
            .trimStart('/')

        val targetName = rawTarget
            .replace('\\', '/')

        if (linkName.isBlank()) return@forEach
        if (targetName.isBlank()) return@forEach
        if (targetName.startsWith("/")) return@forEach

        val linkParts = linkName.split('/')
        if (linkParts.any { it.isBlank() || it == "." || it == ".." }) return@forEach

        val linkFile = normalizedTargetDir.resolve(linkName).normalize()
        if (!linkFile.startsWith(normalizedTargetDir)) return@forEach

        val resolvedTarget = linkFile.parentFile
            ?.resolve(targetName)
            ?.normalize()
            ?: return@forEach

        if (!resolvedTarget.startsWith(normalizedTargetDir)) return@forEach

        linkFile.parentFile?.mkdirs()

        val existingSymlinkTarget = try {
            Os.readlink(linkFile.path)
        } catch (_: Exception) {
            null
        }

        if (existingSymlinkTarget == targetName) {
            return@forEach
        }

        if (linkFile.exists() || existingSymlinkTarget != null) {
            linkFile.delete()
        }

        Os.symlink(targetName, linkFile.path)
    }


}

fun InputStream.unzip(outputStream: OutputStream) {
    ZipInputStream(this).use { zipIn ->
        while (true) {
            val ze = zipIn.nextEntry ?: break

            try {
                if (!ze.isDirectory) {
                    outputStream.write(zipIn.readBytes())
                }
            } finally {
                try {
                    zipIn.closeEntry()
                } catch (_: Exception) {
                }
            }
        }
    }
}

fun File.compressToZip(outputStream: OutputStream) {
    ZipOutputStream(outputStream.buffered()).use { zipOut ->
        walk().forEach { file ->
            if (file.isFile) {
                val zipEntry = ZipEntry(file.toRelativeString(this))
                zipOut.putNextEntry(zipEntry)

                file.inputStream().use { input ->
                    input.copyTo(zipOut)
                }

                zipOut.closeEntry()
            }
        }
    }
}