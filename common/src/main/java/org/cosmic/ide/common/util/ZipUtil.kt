package org.cosmic.ide.common.util

import android.content.Context

import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.Files

object ZipUtil {

    const val BUFFER_SIZE = 1024 * 10

    @JvmStatic
    fun unzipFromAssets(context: Context, zipFile: String, destination: String) {
        try {
            val stream = context.assets.open(zipFile)
            unzip(stream, Paths.get(destination))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun unzip(stream: InputStream, targetDir: Path) {
        ZipInputStream(stream).use { zipIn ->
            for (ze in zipIn.entries()) {
                val resolvedPath = targetDir.resolve(ze.name).normalize()
                if (!resolvedPath.startsWith(targetDir)) {
                    // see: https://snyk.io/research/zip-slip-vulnerability
                    throw SecurityException("Entry with an illegal path: " + ze.name)
                }
                if (ze.isDirectory) {
                    Files.createDirectories(resolvedPath)
                } else {
                    Files.createDirectories(resolvedPath.parent)
                    Files.copy(zipIn, resolvedPath)
                }
            }
        }
        stream.close()
    }
}
