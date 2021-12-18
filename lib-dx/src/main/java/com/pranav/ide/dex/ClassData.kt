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

class ClassData(
    val staticFields: Array<Field>, val instanceFields: Array<Field>,
    val directMethods: Array<Method>, val virtualMethods: Array<Method>
) {
    fun allFields(): Array<Field?> {
        val result = arrayOfNulls<Field>(
            staticFields.size + instanceFields.size
        )
        System.arraycopy(staticFields, 0, result, 0, staticFields.size)
        System.arraycopy(instanceFields, 0, result, staticFields.size, instanceFields.size)
        return result
    }

    fun allMethods(): Array<Method?> {
        val result = arrayOfNulls<Method>(
            directMethods.size + virtualMethods.size
        )
        System.arraycopy(directMethods, 0, result, 0, directMethods.size)
        System.arraycopy(virtualMethods, 0, result, directMethods.size, virtualMethods.size)
        return result
    }

    class Field(val fieldIndex: Int, val accessFlags: Int)
    class Method(val methodIndex: Int, val accessFlags: Int, val codeOffset: Int)
}