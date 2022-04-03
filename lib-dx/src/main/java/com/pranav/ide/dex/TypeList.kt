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

class TypeList(private val dex: Dex?, val types: ShortArray) : Comparable<TypeList> {
    override fun compareTo(other: TypeList): Int {
        var i = 0
        while (i < types.size && i < other.types.size) {
            if (types[i] != other.types[i]) {
                return compare(types[i], other.types[i])
            }
            i++
        }
        return compare(types.size, other.types.size)
    }

    override fun toString(): String {
        val result = StringBuilder()
        result.append("(")
        var i = 0
        val typesLength = types.size
        while (i < typesLength) {
            result.append(if (dex != null) dex.typeNames()[types[i].toInt()] else types[i])
            i++
        }
        result.append(")")
        return result.toString()
    }

    companion object {
        @JvmField
        val EMPTY = TypeList(null, Dex.EMPTY_SHORT_ARRAY)
    }
}
