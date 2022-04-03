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
 * A type definition.
 */
class ClassDef(
    private val buffer: Dex?,
    val offset: Int,
    val typeIndex: Int,
    val accessFlags: Int,
    val supertypeIndex: Int,
    val interfacesOffset: Int,
    val sourceFileIndex: Int,
    val annotationsOffset: Int,
    val classDataOffset: Int,
    val staticValuesOffset: Int
) {
    val interfaces: ShortArray
        get() = buffer!!.readTypeList(interfacesOffset).types

    override fun toString(): String {
        if (buffer == null) {
            return "$typeIndex $supertypeIndex"
        }
        val result = StringBuilder()
        result.append(buffer.typeNames()[typeIndex])
        if (supertypeIndex != NO_INDEX) {
            result.append(" extends ").append(buffer.typeNames()[supertypeIndex])
        }
        return result.toString()
    }

    companion object {
        const val NO_INDEX = -1
    }
}
