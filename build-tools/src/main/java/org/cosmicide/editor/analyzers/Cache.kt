/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.analyzers

import java.io.File
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

object Cache {

    val cacheMap = mutableMapOf<String, JavaFileObject>()
    fun saveCache(file: File): JavaFileObject {
        val obj = object : SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
            private val lastModified = file.lastModified()
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                return file.readText()
            }

            override fun getLastModified(): Long {
                return lastModified
            }
        }
        cacheMap[file.absolutePath] = obj
        return obj
    }

    fun saveCache(obj: JavaFileObject) {
        cacheMap[obj.name] = obj
    }

    fun getCache(key: File): JavaFileObject? {
        return cacheMap[key.absolutePath]
    }
}
