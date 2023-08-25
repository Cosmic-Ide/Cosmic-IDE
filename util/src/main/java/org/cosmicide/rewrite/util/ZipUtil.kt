/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.util

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

fun InputStream.unzip(targetDir: File) {
    ZipInputStream(this).use { zipIn ->
        var ze = zipIn.nextEntry
        while (ze != null) {
            val resolved = targetDir.resolve(ze.name).normalize()
            // Android 14+ prevents Zip Slip attacks: https://developer.android.com/about/versions/14/behavior-changes-14#zip-path-traversal
            if (!resolved.startsWith(targetDir)) {
                // see: https://snyk.io/research/zip-slip-vulnerability
                throw SecurityException("Entry with an illegal path: " + ze.name)
            }
            if (ze.isDirectory) {
                resolved.mkdirs()
            } else {
                resolved.parentFile?.mkdirs()
                resolved.outputStream().use { output ->
                    zipIn.copyTo(output)
                }
            }

            ze = zipIn.nextEntry
        }
    }

    close()
}

fun InputStream.unzip(outputStream: OutputStream) {
    ZipInputStream(this).use { zipIn ->
        var ze = zipIn.nextEntry
        while (ze != null) {
            if (!ze.isDirectory) {
                outputStream.write(zipIn.readBytes())
            }

            ze = zipIn.nextEntry
        }
    }

    close()
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
