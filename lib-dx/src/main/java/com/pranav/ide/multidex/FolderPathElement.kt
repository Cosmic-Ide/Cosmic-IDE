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

import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * A folder element.
 */
internal class FolderPathElement(private val baseFolder: File) : ClassPathElement {
    @Throws(FileNotFoundException::class)
    override fun open(path: String?): InputStream? {
        return FileInputStream(
            File(
                baseFolder,
                path!!.replace(ClassPathElement.SEPARATOR_CHAR, File.separatorChar)
            )
        )
    }

    override fun close() {}
    override fun list(): Iterable<String?>? {
        val result = ArrayList<String?>()
        collect(baseFolder, "", result)
        return result
    }

    private fun collect(folder: File, prefix: String, result: ArrayList<String?>) {
        for (file in folder.listFiles()) {
            if (file.isDirectory) {
                collect(file, prefix + ClassPathElement.SEPARATOR_CHAR + file.name, result)
            } else {
                result.add(prefix + ClassPathElement.SEPARATOR_CHAR + file.name)
            }
        }
    }
}