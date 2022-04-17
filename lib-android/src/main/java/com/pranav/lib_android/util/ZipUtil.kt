package com.pranav.lib_android.util;

import java.io.*
import java.util.zip.ZipFile
import android.content.Context

object ZipUtil {

  const val BUFFER_SIZE = 4096

  @JvmStatic
  fun unzipFromAssets(context: Context, fileName: String, outputDir: String) {
    copyFileFromAssets(context, fileName, outputDir)
    unzip(File(outputDir, fileName), outputDir)
    File(outputDir, fileName).delete()
  }

  fun unzip(zipFilePath: File, destDirectory: String) {
    File(destDirectory).run {
      if (!exists())
        mkdirs()
    }

    ZipFile(zipFilePath).use { zip ->
      zip.entries().asSequence().forEach { entry ->
        zip.getInputStream(entry).use { input ->
          val output = destDirectory + File.separator + entry.name
          if (entry.isDirectory)
            File(output).mkdir()
          else
            extractFile(input, output)
        }
      }
    }
  }

  private fun extractFile(inputStream: InputStream, destFilePath: String) {
    val bos = BufferedOutputStream(FileOutputStream(destFilePath))
    val bytesIn = ByteArray(BUFFER_SIZE)
    var read: Int
    while (inputStream.read(bytesIn).also { read = it } != -1) {
      bos.write(bytesIn, 0, read)
    }
    bos.close()
  }

	fun copyFileFromAssets(context: Context, inputFile: String, outputDir: String) {
		val input = context.getAssets().`open`(inputFile)
		val outputPath = outputDir + inputFile
		val output = FileOutputStream(outputPath)
		input.copyTo(output)
	}
}
