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
package com.pranav.ide.dex.util

import kotlin.experimental.and

/**
 * Unsigned arithmetic over Java's signed types.
 */
object Unsigned {
    fun compare(ushortA: Short, ushortB: Short): Int {
        if (ushortA == ushortB) {
            return 0
        }
        val a: Int = (ushortA and 0xFFFF.toShort()).toInt()
        val b: Int = (ushortB and 0xFFFF.toShort()).toInt()
        return if (a < b) -1 else 1
    }

    @JvmStatic
    fun compare(uintA: Int, uintB: Int): Int {
        if (uintA == uintB) {
            return 0
        }
        val a = (uintA and 0xFFFFFFFFL.toInt()).toLong()
        val b = (uintB and 0xFFFFFFFFL.toInt()).toLong()
        return if (a < b) -1 else 1
    }
}