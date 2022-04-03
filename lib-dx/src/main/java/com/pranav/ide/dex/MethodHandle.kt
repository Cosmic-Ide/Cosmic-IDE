/*
 * Copyright (C) 2017 The Android Open Source Project
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

/**
 * A method_handle_item:
 * https://source.android.com/devices/tech/dalvik/dex-format#method-handle-item
 */
class MethodHandle(
    private val dex: Dex?,
    val methodHandleType: MethodHandleType,
    val unused1: Int,
    val fieldOrMethodId: Int,
    val unused2: Int
) : Comparable<MethodHandle> {
    /**
     * A method handle type code:
     * https://source.android.com/devices/tech/dalvik/dex-format#method-handle-type-codes
     */
    enum class MethodHandleType(internal val value: Int) {
        METHOD_HANDLE_TYPE_STATIC_PUT(0x00),
        METHOD_HANDLE_TYPE_STATIC_GET(0x01),
        METHOD_HANDLE_TYPE_INSTANCE_PUT(
            0x02
        ),
        METHOD_HANDLE_TYPE_INSTANCE_GET(0x03);

        val isField: Boolean
            get() = when (this) {
                METHOD_HANDLE_TYPE_STATIC_PUT,
                METHOD_HANDLE_TYPE_STATIC_GET,
                METHOD_HANDLE_TYPE_INSTANCE_PUT,
                METHOD_HANDLE_TYPE_INSTANCE_GET -> true
            }

        companion object {
            @JvmStatic
            fun fromValue(value: Int): MethodHandleType {
                for (methodHandleType in values()) {
                    if (methodHandleType.value == value) {
                        return methodHandleType
                    }
                }
                throw IllegalArgumentException(value.toString())
            }
        }
    }

    override fun compareTo(other: MethodHandle): Int {
        return if (methodHandleType != other.methodHandleType) {
            methodHandleType.compareTo(other.methodHandleType)
        } else compare(fieldOrMethodId, other.fieldOrMethodId)
    }

    fun writeTo(out: Dex.Section) {
        out.writeUnsignedShort(methodHandleType.value)
        out.writeUnsignedShort(unused1)
        out.writeUnsignedShort(fieldOrMethodId)
        out.writeUnsignedShort(unused2)
    }

    override fun toString(): String {
        return if (dex == null) {
            "$methodHandleType $fieldOrMethodId"
        } else methodHandleType
            .toString() + " " + if (methodHandleType.isField) {
            dex.fieldIds()[fieldOrMethodId]
        } else {
            dex.methodIds()[fieldOrMethodId]
        }
    }
}