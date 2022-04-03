/*
 * Copyright (C) 2011 The Android Open Source Project
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
package com.pranav.ide.dex

import com.pranav.ide.dex.util.ByteInput
import java.io.UTFDataFormatException
import kotlin.experimental.and

/**
 * Modified UTF-8 as described in the dex file format spec.
 */
object Mutf8 {
    /**
     * Decodes bytes from `in` into `out` until a delimiter 0x00 is
     * encountered. Returns a new string containing the decoded characters.
     */
    @JvmStatic
    @Throws(UTFDataFormatException::class)
    fun decode(`in`: ByteInput, out: CharArray): String {
        var s = 0
        while (true) {
            val a: Char = (`in`.readByte() and 0xff.toByte()).toInt().toChar()
            if (a.code == 0) {
                return String(out, 0, s)
            }
            out[s] = a
            if (a < '\u0080') {
                s++
            } else if (a.code and 0xe0 == 0xc0) {
                val b: Int = (`in`.readByte() and 0xff.toByte()).toInt()
                if (b and 0xC0 != 0x80) {
                    throw UTFDataFormatException("bad second byte")
                }
                out[s++] = (a.code and 0x1F shl 6 or (b and 0x3F)).toChar()
            } else if (a.code and 0xf0 == 0xe0) {
                val b: Int = (`in`.readByte() and 0xff.toByte()).toInt()
                val c: Int = (`in`.readByte() and 0xff.toByte()).toInt()
                if (b and 0xC0 != 0x80 || c and 0xC0 != 0x80) {
                    throw UTFDataFormatException("bad second or third byte")
                }
                out[s++] = (a.code and 0x0F shl 12 or (b and 0x3F shl 6) or (c and 0x3F)).toChar()
            } else {
                throw UTFDataFormatException("bad byte")
            }
        }
    }

    /**
     * Returns the number of bytes the modified UTF8 representation of 's' would take.
     */
    @Throws(UTFDataFormatException::class)
    private fun countBytes(s: String, shortLength: Boolean): Long {
        var result: Long = 0
        val length = s.length
        for (i in 0 until length) {
            val ch = s[i]
            if (ch.code != 0 && ch.code <= 127) { // U+0000 uses two bytes.
                ++result
            } else if (ch.code <= 2047) {
                result += 2
            } else {
                result += 3
            }
            if (shortLength && result > 65535) {
                throw UTFDataFormatException("String more than 65535 UTF bytes long")
            }
        }
        return result
    }

    /**
     * Encodes the modified UTF-8 bytes corresponding to `s` into  `dst`, starting at `offset`.
     */
    fun encode(dst: ByteArray, off: Int, s: String) {
        var offset = off
        val length = s.length
        for (i in 0 until length) {
            val ch = s[i]
            if (ch.code != 0 && ch.code <= 127) { // U+0000 uses two bytes.
                dst[offset++] = ch.code.toByte()
            } else if (ch.code <= 2047) {
                dst[offset++] = (0xc0 or (0x1f and (ch.code shr 6))).toByte()
                dst[offset++] = (0x80 or (0x3f and ch.code)).toByte()
            } else {
                dst[offset++] = (0xe0 or (0x0f and (ch.code shr 12))).toByte()
                dst[offset++] = (0x80 or (0x3f and (ch.code shr 6))).toByte()
                dst[offset++] = (0x80 or (0x3f and ch.code)).toByte()
            }
        }
    }

    /**
     * Returns an array containing the *modified UTF-8* form of `s`.
     */
    @JvmStatic
    @Throws(UTFDataFormatException::class)
    fun encode(s: String): ByteArray {
        val utfCount = countBytes(s, true).toInt()
        val result = ByteArray(utfCount)
        encode(result, 0, s)
        return result
    }
}
