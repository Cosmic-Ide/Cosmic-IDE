/*
 * Copyright (C) 2015 Pedro Vicente Gomez Sanchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pedrovgs.lynx.model

import android.util.Log
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Logcat abstraction created to be able to read from the device log output. This implementation is
 * based on a BufferReader connected to the process InputStream you can obtain executing a command
 * using Android Runtime object.
 *
 *
 * This class will notify listeners configured previously about new traces sent to the device and
 * will be reading and notifying traces until stopReading() method be invoked.
 *
 *
 * To be able to read from a process InputStream without block the thread where we were, this
 * class extends from Thread and all the code inside the run() method will be executed in a
 * background thread.
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
class Logcat : Thread() {
    private var process: Process? = null
    private var bufferReader: BufferedReader? = null
        get() {
            if (field == null) {
                field = BufferedReader(InputStreamReader(process!!.inputStream))
            }
            return field
        }

    /**
     * The logcat listener.
     */
    var listener: Listener? = null
    private var continueReading = true

    /** Starts reading traces from the application logcat and notifying listeners if needed.  */
    override fun run() {
        super.run()
        try {
            process = Runtime.getRuntime().exec("logcat -v brief")
        } catch (e: IOException) {
            Log.e(LOGTAG, "IOException executing logcat command.", e)
        }
        readLogcat()
    }

    /** Stops reading from the application logcat and notifying listeners.  */
    fun stopReading() {
        continueReading = false
    }

    private fun readLogcat() {
        val bufferedReader = bufferReader!!
        try {
            var trace = bufferedReader.readLine()
            while (trace != null && continueReading) {
                notifyListener(trace)
                trace = bufferedReader.readLine()
            }
        } catch (e: IOException) {
            Log.e(LOGTAG, "IOException reading logcat trace.", e)
        }
    }

    private fun notifyListener(trace: String) {
        if (listener != null) {
            listener!!.onTraceRead(trace)
        }
    }

    public override fun clone(): Any {
        return Logcat()
    }

    interface Listener {
        fun onTraceRead(logcatTrace: String?)
    }

    companion object {
        private const val LOGTAG = "Logcat"
    }
}
