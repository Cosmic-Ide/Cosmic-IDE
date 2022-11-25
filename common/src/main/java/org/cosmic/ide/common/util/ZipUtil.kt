package org.cosmic.ide.common.util

import android.content.Context

import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.io.File

object ZipUtil {

    const val BUFFER_SIZE = 2048 * 10

    @JvmStatic
    fun unzipFromAssets(context: Context, zipFile: String, destination: String) {
        try {
            val stream = context.assets.open(zipFile)
            unzip(stream, File(destination))
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun unzip(stream: InputStream, targetDir: File) {
        ZipInputStream(stream).use { zipIn ->
            var ze: ZipEntry? = zipIn.nextEntry
            while (ze != null) {
              val resolved = targetDir.resolve(ze.name).normalize()
              if (!resolved.startsWith(targetDir)) {
                    // see: https://snyk.io/research/zip-slip-vulnerability
                    throw SecurityException("Entry with an illegal path: " + ze.name)
                }
                if (ze.isDirectory) {
                    resolved.mkdirs()
                } else {
                    resolved.parentFile.mkdirs()
                    resolved.outputStream().use { output ->
                        zipIn.copyTo(output)
                    }
                }

                ze = zipIn.nextEntry
            }
        }

        stream.close()
    }
}
