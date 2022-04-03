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

import com.pranav.ide.dex.util.ByteArrayByteInput
import com.pranav.ide.dex.util.ByteInput
import kotlin.experimental.and

/**
 * An encoded value or array.
 */
class EncodedValue(val bytes: ByteArray) : Comparable<EncodedValue> {
    fun asByteInput(): ByteInput {
        return ByteArrayByteInput(*bytes)
    }

    fun writeTo(out: Dex.Section) {
        out.write(bytes)
    }

    override fun compareTo(other: EncodedValue): Int {
        val size = bytes.size.coerceAtMost(other.bytes.size)
        for (i in 0 until size) {
            if (bytes[i] != other.bytes[i]) {
                return (bytes[i] and 0xff.toByte()) - (other.bytes[i] and 0xff.toByte())
            }
        }
        return bytes.size - other.bytes.size
    }

    override fun toString(): String {
        return Integer.toHexString((bytes[0] and 0xff.toByte()).toInt()) + "...(" + bytes.size + ")"
    }
}
