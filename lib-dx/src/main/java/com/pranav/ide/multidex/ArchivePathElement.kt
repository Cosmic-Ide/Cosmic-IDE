/*
 * Copyright (C) 2013 The Android Open Source Project
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
package com.pranav.ide.multidex

import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * A zip element.
 */
internal class ArchivePathElement(private val archive: ZipFile) : ClassPathElement {
    internal class DirectoryEntryException : IOException()

    @Throws(IOException::class)
    override fun open(path: String?): InputStream? {
        val entry = archive.getEntry(path)
        return if (entry == null) {
            throw FileNotFoundException("File \"$path\" not found")
        } else if (entry.isDirectory) {
            throw DirectoryEntryException()
        } else {
            archive.getInputStream(entry)
        }
    }

    @Throws(IOException::class)
    override fun close() {
        archive.close()
    }

    override fun list(): Iterable<String?> {
        return object : Iterable<String?> {
            override fun iterator(): MutableIterator<String?> {
                return object : MutableIterator<String?> {
                    val delegate = archive.entries()
                    var next: ZipEntry? = null
                    override fun hasNext(): Boolean {
                        while (next == null && delegate.hasMoreElements()) {
                            val el = delegate.nextElement()
                            next = if (el.isDirectory) {
                                null
                            } else {
                                el
                            }
                        }
                        return next != null
                    }

                    override fun next(): String {
                        return if (hasNext()) {
                            val name = next!!.name
                            next = null
                            name
                        } else {
                            throw NoSuchElementException()
                        }
                    }

                    override fun remove() {
                        throw UnsupportedOperationException()
                    }
                }
            }
        }
    }
}