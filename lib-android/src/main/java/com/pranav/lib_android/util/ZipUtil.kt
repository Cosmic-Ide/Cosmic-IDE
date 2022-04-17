package com.pranav.lib_android.util;

import java.io.*
import java.util.zip.ZipFile
import android.content.Context

object ZipUtil {

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
    val bos = FileOutputStream(destFilePath)
    inputStream.copyTo(bos)
  }

	fun copyFileFromAssets(context: Context, inputFile: String, outputDir: String) {
		val input = context.getAssets().`open`(inputFile)
		val outputPath = outputDir + inputFile
		val output = FileOutputStream(outputPath)
		input.copyTo(output)
	}
}
