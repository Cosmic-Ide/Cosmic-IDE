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

import com.pranav.ide.dex.util.ByteInput
import kotlin.experimental.and

/**
 * Pull parser for encoded values.
 */
class EncodedValueReader {
    private val `in`: ByteInput
    private var type = MUST_READ

    /**
     * Returns the type of the annotation just returned by [ ][.readAnnotation]. This method's value is undefined unless the most
     * recent call was to [.readAnnotation].
     */
    var annotationType = 0
        private set
    private var arg = 0

    constructor(`in`: ByteInput) {
        this.`in` = `in`
    }

    constructor(`in`: EncodedValue) : this(`in`.asByteInput())

    /**
     * Creates a new encoded value reader whose only value is the specified
     * known type. This is useful for encoded values without a type prefix,
     * such as class_def_item's encoded_array or annotation_item's
     * encoded_annotation.
     */
    constructor(`in`: ByteInput, knownType: Int) {
        this.`in` = `in`
        type = knownType
    }

    constructor(`in`: EncodedValue, knownType: Int) : this(`in`.asByteInput(), knownType)

    /**
     * Returns the type of the next value to read.
     */
    fun peek(): Int {
        if (type == MUST_READ) {
            val argAndType: Int = (`in`.readByte() and 0xff.toByte()).toInt()
            type = argAndType and 0x1f
            arg = argAndType and 0xe0 shr 5
        }
        return type
    }

    /**
     * Begins reading the elements of an array, returning the array's size. The
     * caller must follow up by calling a read method for each element in the
     * array. For example, this reads a byte array: <pre>   `int arraySize = readArray();
     * for (int i = 0, i < arraySize; i++) {
     * readByte();
     * }
    `</pre> *
     */
    fun readArray(): Int {
        checkType(ENCODED_ARRAY)
        type = MUST_READ
        return Leb128.readUnsignedLeb128(`in`)
    }

    /**
     * Begins reading the fields of an annotation, returning the number of
     * fields. The caller must follow up by making alternating calls to [ ][.readAnnotationName] and another read method. For example, this reads
     * an annotation whose fields are all bytes: <pre>   `int fieldCount = readAnnotation();
     * int annotationType = getAnnotationType();
     * for (int i = 0; i < fieldCount; i++) {
     * readAnnotationName();
     * readByte();
     * }
    `</pre> *
     */
    fun readAnnotation(): Int {
        checkType(ENCODED_ANNOTATION)
        type = MUST_READ
        annotationType = Leb128.readUnsignedLeb128(`in`)
        return Leb128.readUnsignedLeb128(`in`)
    }

    fun readAnnotationName(): Int {
        return Leb128.readUnsignedLeb128(`in`)
    }

    fun readByte(): Byte {
        checkType(ENCODED_BYTE)
        type = MUST_READ
        return EncodedValueCodec.readSignedInt(`in`, arg).toByte()
    }

    fun readShort(): Short {
        checkType(ENCODED_SHORT)
        type = MUST_READ
        return EncodedValueCodec.readSignedInt(`in`, arg).toShort()
    }

    fun readChar(): Char {
        checkType(ENCODED_CHAR)
        type = MUST_READ
        return EncodedValueCodec.readUnsignedInt(`in`, arg, false).toChar()
    }

    fun readInt(): Int {
        checkType(ENCODED_INT)
        type = MUST_READ
        return EncodedValueCodec.readSignedInt(`in`, arg)
    }

    fun readLong(): Long {
        checkType(ENCODED_LONG)
        type = MUST_READ
        return EncodedValueCodec.readSignedLong(`in`, arg)
    }

    fun readFloat(): Float {
        checkType(ENCODED_FLOAT)
        type = MUST_READ
        return java.lang.Float.intBitsToFloat(EncodedValueCodec.readUnsignedInt(`in`, arg, true))
    }

    fun readDouble(): Double {
        checkType(ENCODED_DOUBLE)
        type = MUST_READ
        return java.lang.Double.longBitsToDouble(
            EncodedValueCodec.readUnsignedLong(
                `in`,
                arg,
                true
            )
        )
    }

    fun readMethodType(): Int {
        checkType(ENCODED_METHOD_TYPE)
        type = MUST_READ
        return EncodedValueCodec.readUnsignedInt(`in`, arg, false)
    }

    fun readMethodHandle(): Int {
        checkType(ENCODED_METHOD_HANDLE)
        type = MUST_READ
        return EncodedValueCodec.readUnsignedInt(`in`, arg, false)
    }

    fun readString(): Int {
        checkType(ENCODED_STRING)
        type = MUST_READ
        return EncodedValueCodec.readUnsignedInt(`in`, arg, false)
    }

    fun readType(): Int {
        checkType(ENCODED_TYPE)
        type = MUST_READ
        return EncodedValueCodec.readUnsignedInt(`in`, arg, false)
    }

    fun readField(): Int {
        checkType(ENCODED_FIELD)
        type = MUST_READ
        return EncodedValueCodec.readUnsignedInt(`in`, arg, false)
    }

    fun readEnum(): Int {
        checkType(ENCODED_ENUM)
        type = MUST_READ
        return EncodedValueCodec.readUnsignedInt(`in`, arg, false)
    }

    fun readMethod(): Int {
        checkType(ENCODED_METHOD)
        type = MUST_READ
        return EncodedValueCodec.readUnsignedInt(`in`, arg, false)
    }

    fun readNull() {
        checkType(ENCODED_NULL)
        type = MUST_READ
    }

    fun readBoolean(): Boolean {
        checkType(ENCODED_BOOLEAN)
        type = MUST_READ
        return arg != 0
    }

    /**
     * Skips a single value, including its nested values if it is an array or
     * annotation.
     */
    fun skipValue() {
        when (peek()) {
            ENCODED_BYTE -> readByte()
            ENCODED_SHORT -> readShort()
            ENCODED_CHAR -> readChar()
            ENCODED_INT -> readInt()
            ENCODED_LONG -> readLong()
            ENCODED_FLOAT -> readFloat()
            ENCODED_DOUBLE -> readDouble()
            ENCODED_METHOD_TYPE -> readMethodType()
            ENCODED_METHOD_HANDLE -> readMethodHandle()
            ENCODED_STRING -> readString()
            ENCODED_TYPE -> readType()
            ENCODED_FIELD -> readField()
            ENCODED_ENUM -> readEnum()
            ENCODED_METHOD -> readMethod()
            ENCODED_ARRAY -> {
                var i = 0
                val size = readArray()
                while (i < size) {
                    skipValue()
                    i++
                }
            }
            ENCODED_ANNOTATION -> {
                var i = 0
                val size = readAnnotation()
                while (i < size) {
                    readAnnotationName()
                    skipValue()
                    i++
                }
            }
            ENCODED_NULL -> readNull()
            ENCODED_BOOLEAN -> readBoolean()
            else -> throw DexException("Unexpected type: " + Integer.toHexString(type))
        }
    }

    private fun checkType(expected: Int) {
        check(peek() == expected) {
            String.format(
                "Expected %x but was %x",
                expected,
                peek()
            )
        }
    }

    companion object {
        const val ENCODED_BYTE = 0x00
        const val ENCODED_SHORT = 0x02
        const val ENCODED_CHAR = 0x03
        const val ENCODED_INT = 0x04
        const val ENCODED_LONG = 0x06
        const val ENCODED_FLOAT = 0x10
        const val ENCODED_DOUBLE = 0x11
        const val ENCODED_METHOD_TYPE = 0x15
        const val ENCODED_METHOD_HANDLE = 0x16
        const val ENCODED_STRING = 0x17
        const val ENCODED_TYPE = 0x18
        const val ENCODED_FIELD = 0x19
        const val ENCODED_ENUM = 0x1b
        const val ENCODED_METHOD = 0x1a
        const val ENCODED_ARRAY = 0x1c
        const val ENCODED_ANNOTATION = 0x1d
        const val ENCODED_NULL = 0x1e
        const val ENCODED_BOOLEAN = 0x1f

        /** placeholder type if the type is not yet known  */
        private const val MUST_READ = -1
    }
}