package com.pranav.lib_android.util

import java.io.PrintWriter
import java.io.StringWriter
import java.util.ArrayList
import java.util.Scanner

class BinaryExecutor {

  var mProcess = ProcessBuilder()
  var mWriter = StringWriter()

  fun execute(arrayList: ArrayList<String>): String {
		mProcess.command(arrayList)
    try {
			val process = mProcess.start()
      val scanner = Scanner(process.getErrorStream())
      while (scanner.hasNextLine()) {
        mWriter.append(scanner.nextLine())
        mWriter.append(System.lineSeparator())
      }

			process.waitFor()
    } catch (e: Exception) {
      e.printStackTrace(PrintWriter(mWriter))
    }
    return mWriter.toString()
  }

	fun execute(command: String): String {
		mProcess.command(command)
    try {
			val process = mProcess.start()
      val scanner = Scanner(process.getErrorStream())
      while (scanner.hasNextLine()) {
        mWriter.append(scanner.nextLine())
        mWriter.append(System.lineSeparator())
      }

			process.waitFor()
    } catch (e: Exception) {
      e.printStackTrace(PrintWriter(mWriter))
    }
    return mWriter.toString()
	}

  fun getLogs(): String = mWriter.toString()

	fun clear() {
		mWriter.flush()
	}
}
