/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pranav.ide.dex.util

import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * File I/O utilities.
 */
object FileUtils {
    /**
     * Reads the named file, translating [IOException] to a
     * [RuntimeException] of some sort.
     *
     * @param fileName `non-null;` name of the file to read
     * @return `non-null;` contents of the file
     */
    @JvmStatic
    fun readFile(fileName: String): ByteArray {
        val file = File(fileName)
        return readFile(file)
    }

    /**
     * Reads the given file, translating [IOException] to a
     * [RuntimeException] of some sort.
     *
     * @param file `non-null;` the file to read
     * @return `non-null;` contents of the file
     */
    @JvmStatic
    fun readFile(file: File): ByteArray {
        if (!file.exists()) {
            throw RuntimeException("$file: file not found")
        }
        if (!file.isFile) {
            throw RuntimeException("$file: not a file")
        }
        if (!file.canRead()) {
            throw RuntimeException("$file: file not readable")
        }
        val longLength = file.length()
        var length = longLength.toInt()
        if (length.toLong() != longLength) {
            throw RuntimeException("$file: file too long")
        }
        val result = ByteArray(length)
        try {
            val `in` = FileInputStream(file)
            var at = 0
            while (length > 0) {
                val amt = `in`.read(result, at, length)
                if (amt == -1) {
                    throw RuntimeException("$file: unexpected EOF")
                }
                at += amt
                length -= amt
            }
            `in`.close()
        } catch (ex: IOException) {
            throw RuntimeException("$file: trouble reading", ex)
        }
        return result
    }

    /**
     * Returns true if `fileName` names a .zip, .jar, or .apk.
     */
    @JvmStatic
    fun hasArchiveSuffix(fileName: String): Boolean {
        return (
            fileName.endsWith(".zip") ||
            fileName.endsWith(".jar") ||
            fileName.endsWith(".apk")
        )
    }
}
