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

/**
 * An annotation.
 */
class Annotation(
    private val dex: Dex?,
    val visibility: Byte,
    private val encodedAnnotation: EncodedValue
) : Comparable<Annotation> {
    val reader: EncodedValueReader
        get() = EncodedValueReader(encodedAnnotation, EncodedValueReader.ENCODED_ANNOTATION)
    val typeIndex: Int
        get() {
            val reader = reader
            reader.readAnnotation()
            return reader.annotationType
        }

    fun writeTo(out: Dex.Section) {
        out.writeByte(visibility.toInt())
        encodedAnnotation.writeTo(out)
    }

    override fun compareTo(other: Annotation): Int {
        return encodedAnnotation.compareTo(other.encodedAnnotation)
    }

    override fun toString(): String {
        return if (dex == null) "$visibility $typeIndex" else "$visibility " + dex.typeNames()[typeIndex]
    }
}