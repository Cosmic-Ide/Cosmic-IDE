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

import com.pranav.ide.dex.util.Unsigned.compare

class FieldId(
    private val dex: Dex?,
    val declaringClassIndex: Int,
    val typeIndex: Int,
    val nameIndex: Int
) : Comparable<FieldId> {
    override fun compareTo(other: FieldId): Int {
        if (declaringClassIndex != other.declaringClassIndex) {
            return compare(declaringClassIndex, other.declaringClassIndex)
        }
        return if (nameIndex != other.nameIndex) {
            compare(nameIndex, other.nameIndex)
        } else compare(typeIndex, other.typeIndex)
        // should always be 0
    }

    fun writeTo(out: Dex.Section) {
        out.writeUnsignedShort(declaringClassIndex)
        out.writeUnsignedShort(typeIndex)
        out.writeInt(nameIndex)
    }

    override fun toString(): String {
        return if (dex == null) {
            "$declaringClassIndex $typeIndex $nameIndex"
        } else dex.typeNames()[typeIndex].toString() + "." + dex.strings()[nameIndex]
    }
}