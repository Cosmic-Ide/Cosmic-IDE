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

import com.pranav.ide.dex.DexFormat.apiToMagic
import com.pranav.ide.dex.DexFormat.isSupportedDexMagic
import com.pranav.ide.dex.DexFormat.magicToApi
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * The file header and map.
 */
class TableOfContents {
    /*
     * TODO: factor out ID constants.
     */
    @JvmField
    val header = Section(0x0000)

    @JvmField
    val stringIds = Section(0x0001)

    @JvmField
    val typeIds = Section(0x0002)

    @JvmField
    val protoIds = Section(0x0003)

    @JvmField
    val fieldIds = Section(0x0004)

    @JvmField
    val methodIds = Section(0x0005)

    @JvmField
    val classDefs = Section(0x0006)

    @JvmField
    val callSiteIds = Section(0x0007)

    @JvmField
    val methodHandles = Section(0x0008)

    @JvmField
    val mapList = Section(0x1000)

    @JvmField
    val typeLists = Section(0x1001)

    @JvmField
    val annotationSetRefLists = Section(0x1002)

    @JvmField
    val annotationSets = Section(0x1003)

    @JvmField
    val classData = Section(0x2000)

    @JvmField
    val codes = Section(0x2001)

    @JvmField
    val stringData = Section(0x2002)

    @JvmField
    val debugInfo = Section(0x2003)

    @JvmField
    val annotations = Section(0x2004)

    @JvmField
    val encodedArrays = Section(0x2005)

    @JvmField
    val annotationsDirectories = Section(0x2006)

    @JvmField
    val sections = arrayOf(
        header, stringIds, typeIds, protoIds, fieldIds, methodIds, classDefs, mapList, callSiteIds,
        methodHandles, typeLists, annotationSetRefLists, annotationSets, classData, codes,
        stringData, debugInfo, annotations, encodedArrays, annotationsDirectories
    )

    @JvmField
    var apiLevel = 0
    var checksum = 0
    var signature: ByteArray

    @JvmField
    var fileSize = 0
    private var linkSize = 0
    var linkOff = 0

    @JvmField
    var dataSize = 0

    @JvmField
    var dataOff = 0

    @Throws(IOException::class)
    fun readFrom(dex: Dex) {
        readHeader(dex.open(0))
        readMap(dex.open(mapList.off))
        computeSizesFromOffsets()
    }

    @Throws(UnsupportedEncodingException::class)
    private fun readHeader(headerIn: Dex.Section) {
        val magic = headerIn.readByteArray(8)
        if (!isSupportedDexMagic(magic)) {
            val msg = String.format(
                "Unexpected magic: [0x%02x, 0x%02x, 0x%02x, 0x%02x, " +
                    "0x%02x, 0x%02x, 0x%02x, 0x%02x]",
                magic[0], magic[1], magic[2], magic[3],
                magic[4], magic[5], magic[6], magic[7]
            )
            throw DexException(msg)
        }
        apiLevel = magicToApi(magic)
        checksum = headerIn.readInt()
        signature = headerIn.readByteArray(20)
        fileSize = headerIn.readInt()
        val headerSize = headerIn.readInt()
        if (headerSize != SizeOf.HEADER_ITEM) {
            throw DexException("Unexpected header: 0x" + Integer.toHexString(headerSize))
        }
        val endianTag = headerIn.readInt()
        if (endianTag != DexFormat.ENDIAN_TAG) {
            throw DexException("Unexpected endian tag: 0x" + Integer.toHexString(endianTag))
        }
        linkSize = headerIn.readInt()
        linkOff = headerIn.readInt()
        mapList.off = headerIn.readInt()
        if (mapList.off == 0) {
            throw DexException("Cannot merge dex files that do not contain a map")
        }
        stringIds.size = headerIn.readInt()
        stringIds.off = headerIn.readInt()
        typeIds.size = headerIn.readInt()
        typeIds.off = headerIn.readInt()
        protoIds.size = headerIn.readInt()
        protoIds.off = headerIn.readInt()
        fieldIds.size = headerIn.readInt()
        fieldIds.off = headerIn.readInt()
        methodIds.size = headerIn.readInt()
        methodIds.off = headerIn.readInt()
        classDefs.size = headerIn.readInt()
        classDefs.off = headerIn.readInt()
        dataSize = headerIn.readInt()
        dataOff = headerIn.readInt()
    }

    @Throws(IOException::class)
    private fun readMap(`in`: Dex.Section) {
        val mapSize = `in`.readInt()
        var previous: Section? = null
        for (i in 0 until mapSize) {
            val type = `in`.readShort()
            `in`.readShort() // unused
            val section = getSection(type)
            val size = `in`.readInt()
            val offset = `in`.readInt()
            if (section.size != 0 && section.size != size ||
                section.off != -1 && section.off != offset
            ) {
                throw DexException(
                    "Unexpected map value for 0x" +
                    Integer.toHexString(type.toInt())
                )
            }
            section.size = size
            section.off = offset
            if (previous != null && previous.off > section.off) {
                throw DexException("Map is unsorted at $previous, $section")
            }
            previous = section
        }
        Arrays.sort(sections)
    }

    fun computeSizesFromOffsets() {
        var end = dataOff + dataSize
        for (i in sections.indices.reversed()) {
            val section = sections[i]
            if (section.off == -1) {
                continue
            }
            if (section.off > end) {
                throw DexException("Map is unsorted at $section")
            }
            section.byteCount = end - section.off
            end = section.off
        }
    }

    private fun getSection(type: Short): Section {
        for (section in sections) {
            if (section.type == type) {
                return section
            }
        }
        throw IllegalArgumentException("No such map item: $type")
    }

    @Throws(IOException::class)
    fun writeHeader(out: Dex.Section, api: Int) {
        out.write(apiToMagic(api).toByteArray(StandardCharsets.UTF_8))
        out.writeInt(checksum)
        out.write(signature)
        out.writeInt(fileSize)
        out.writeInt(SizeOf.HEADER_ITEM)
        out.writeInt(DexFormat.ENDIAN_TAG)
        out.writeInt(linkSize)
        out.writeInt(linkOff)
        out.writeInt(mapList.off)
        out.writeInt(stringIds.size)
        out.writeInt(stringIds.off)
        out.writeInt(typeIds.size)
        out.writeInt(typeIds.off)
        out.writeInt(protoIds.size)
        out.writeInt(protoIds.off)
        out.writeInt(fieldIds.size)
        out.writeInt(fieldIds.off)
        out.writeInt(methodIds.size)
        out.writeInt(methodIds.off)
        out.writeInt(classDefs.size)
        out.writeInt(classDefs.off)
        out.writeInt(dataSize)
        out.writeInt(dataOff)
    }

    @Throws(IOException::class)
    fun writeMap(out: Dex.Section) {
        var count = 0
        for (section in sections) {
            if (section.exists()) {
                count++
            }
        }
        out.writeInt(count)
        for (section in sections) {
            if (section.exists()) {
                out.writeShort(section.type)
                out.writeShort(0.toShort())
                out.writeInt(section.size)
                out.writeInt(section.off)
            }
        }
    }

    class Section(type: Int) : Comparable<Section> {
        @JvmField
        val type: Short

        @JvmField
        var size = 0

        @JvmField
        var off = -1

        @JvmField
        var byteCount = 0
        fun exists(): Boolean {
            return size > 0
        }

        override fun compareTo(other: Section): Int {
            return if (off != other.off) {
                if (off < other.off) -1 else 1
            } else 0
        }

        override fun toString(): String {
            return String.format("Section[type=%#x,off=%#x,size=%#x]", type, off, size)
        }

        init {
            this.type = type.toShort()
        }
    }

    init {
        signature = ByteArray(20)
    }
}
