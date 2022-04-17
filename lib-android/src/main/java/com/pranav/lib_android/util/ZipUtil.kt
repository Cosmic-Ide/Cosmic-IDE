package com.pranav.lib_android.util;

import java.io.*
import java.util.zip.ZipFile
import android.content.Context

class ZipUtil {

  const val BUFFER_SIZE = 4096

  @JvmStatic
  fun unzipFromAssets(context: Context, fileName: String, outputDir: String) {
    copyFileFromAssets(context, fileName, outputDir)
    unzip(File(outputDir, fileName), outputDir)
    File(outputDir, fileName).delete()
  }

  @JvmStatic
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
  @JvmStatic
  fun extractFile(inputStream: InputStream, destFilePath: String) {
    val bos = BufferedOutputStream(FileOutputStream(destFilePath))
    val bytesIn = ByteArray(BUFFER_SIZE)
    var read: Int
    for (Byte byte : inputStream.readBytes()) {
      bos.write(byte, 0, read)
    }
    bos.close()
  }
  @JvmStatic
	fun copyFileFromAssets(context: Context, inputFile: String, outputDir: String) {
		val input = context.getAssets().`open`(inputFile)
		val outputPath = outputDir + inputFile
		val file = File(outputPath)
		if (file.createNewFile()) {
		  file.writeBytes(input.readBytes())
		}
	}
}
