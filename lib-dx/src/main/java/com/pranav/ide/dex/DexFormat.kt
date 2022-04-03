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
 * Constants that show up in and are otherwise related to `.dex`
 * files, and helper methods for same.
 */
object DexFormat {
    /** API level to target in order to allow spaces in SimpleName  */
    private const val API_SPACES_IN_SIMPLE_NAME = 10000

    /** API level to target in order to generate const-method-handle and const-method-type  */
    const val API_CONST_METHOD_HANDLE = 28

    /** API level to target in order to generate invoke-polymorphic and invoke-custom  */
    const val API_METHOD_HANDLES = 26

    /** API level to target in order to define default and static interface methods  */
    const val API_DEFINE_INTERFACE_METHODS = 24

    /** API level to target in order to invoke default and static interface methods  */
    const val API_INVOKE_INTERFACE_METHODS = 24

    /** API level at which the invocation of static interface methods is permitted by dx.
     * This value has been determined experimentally by testing on different VM versions.  */
    const val API_INVOKE_STATIC_INTERFACE_METHODS = 21

    /** API level to target in order to suppress extended opcode usage  */
    const val API_NO_EXTENDED_OPCODES = 13

    /**
     * API level to target in order to produce the most modern file
     * format
     */
    private const val API_CURRENT = API_CONST_METHOD_HANDLE

    /** dex file version number for API level 10000 and earlier  */
    private const val VERSION_FOR_API_10000 = "040"

    /** dex file version number for API level 28 and earlier  */
    private const val VERSION_FOR_API_28 = "039"

    /** dex file version number for API level 26 and earlier  */
    private const val VERSION_FOR_API_26 = "038"

    /** dex file version number for API level 24 and earlier  */
    private const val VERSION_FOR_API_24 = "037"

    /** dex file version number for API level 13 and earlier  */
    private const val VERSION_FOR_API_13 = "035"

    /**
     * Dex file version number for dalvik.
     *
     *
     * Note: Dex version 36 was loadable in some versions of Dalvik but was never fully supported or
     * completed and is not considered a valid dex file format.
     *
     */
    private const val VERSION_CURRENT = VERSION_FOR_API_28

    /**
     * file name of the primary `.dex` file inside an
     * application or library `.jar` file
     */
    const val DEX_IN_JAR_NAME = "classes.dex"

    /** common prefix for all dex file "magic numbers"  */
    private const val MAGIC_PREFIX = "dex\n"

    /** common suffix for all dex file "magic numbers"  */
    private const val MAGIC_SUFFIX = "\u0000"

    /**
     * value used to indicate endianness of file contents
     */
    const val ENDIAN_TAG = 0x12345678

    /**
     * Maximum addressable field or method index.
     * The largest addressable member is 0xffff, in the "instruction formats" spec as field@CCCC or
     * meth@CCCC.
     */
    const val MAX_MEMBER_IDX = 0xFFFF

    /**
     * Maximum addressable type index.
     * The largest addressable type is 0xffff, in the "instruction formats" spec as type@CCCC.
     */
    const val MAX_TYPE_IDX = 0xFFFF

    /**
     * Returns the API level corresponding to the given magic number,
     * or `-1` if the given array is not a well-formed dex file
     * magic number.
     *
     * @param magic array of bytes containing DEX file magic string
     * @return API level corresponding to magic string if valid, -1 otherwise.
     */
    @JvmStatic
    fun magicToApi(magic: ByteArray): Int {
        if (magic.size != 8) {
            return -1
        }
        if (magic[0] != 'd'.code.toByte() ||
            magic[1] != 'e'.code.toByte() ||
            magic[2] != 'x'.code.toByte() ||
            magic[3] != '\n'.code.toByte() ||
            magic[7] != '\u0000'.code.toByte()
        ) {
            return -1
        }
        val version = "" + magic[4].toInt().toChar() + magic[5]
            .toInt().toChar() + magic[6].toInt().toChar()
        return when (version) {
            VERSION_FOR_API_13 -> API_NO_EXTENDED_OPCODES
            VERSION_FOR_API_24 -> API_DEFINE_INTERFACE_METHODS
            VERSION_FOR_API_26 -> API_METHOD_HANDLES
            VERSION_FOR_API_28 -> API_CONST_METHOD_HANDLE
            VERSION_FOR_API_10000 -> API_SPACES_IN_SIMPLE_NAME
            else -> -1
        }
    }

    /**
     * Returns the magic number corresponding to the given target API level.
     *
     * @param targetApiLevel level of API (minimum supported value 13).
     * @return Magic string corresponding to API level supplied.
     */
    @JvmStatic
    fun apiToMagic(targetApiLevel: Int): String {
        val version: String = if (targetApiLevel >= API_CURRENT) {
            VERSION_CURRENT
        } else if (targetApiLevel >= API_SPACES_IN_SIMPLE_NAME) {
            VERSION_FOR_API_10000
        } else if (targetApiLevel >= API_CONST_METHOD_HANDLE) {
            VERSION_FOR_API_28
        } else if (targetApiLevel >= API_METHOD_HANDLES) {
            VERSION_FOR_API_26
        } else if (targetApiLevel >= API_DEFINE_INTERFACE_METHODS) {
            VERSION_FOR_API_24
        } else {
            VERSION_FOR_API_13
        }
        return MAGIC_PREFIX + version + MAGIC_SUFFIX
    }

    /**
     * Checks whether a DEX file magic string is supported.
     * @param magic string from DEX file
     * @return
     */
    @JvmStatic
    fun isSupportedDexMagic(magic: ByteArray): Boolean {
        val api = magicToApi(magic)
        return api > 0
    }
}
