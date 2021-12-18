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

class ProtoId(
    private val dex: Dex?,
    val shortyIndex: Int,
    val returnTypeIndex: Int,
    val parametersOffset: Int
) : Comparable<ProtoId> {
    override fun compareTo(other: ProtoId): Int {
        return if (returnTypeIndex != other.returnTypeIndex) {
            compare(returnTypeIndex, other.returnTypeIndex)
        } else compare(
            parametersOffset,
            other.parametersOffset
        )
    }

    fun writeTo(out: Dex.Section) {
        out.writeInt(shortyIndex)
        out.writeInt(returnTypeIndex)
        out.writeInt(parametersOffset)
    }

    override fun toString(): String {
        return if (dex == null) {
            "$shortyIndex $returnTypeIndex $parametersOffset"
        } else dex.strings()[shortyIndex]
            .toString() + ": " + dex.typeNames()[returnTypeIndex] + " " + dex.readTypeList(
            parametersOffset
        )
    }
}