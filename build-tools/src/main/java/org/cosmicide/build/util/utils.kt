/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.build.util

import org.cosmicide.rewrite.util.FileUtil
import java.io.File

/**
 * Returns a list of files with the given extension within the specified directory and its subdirectories.
 *
 * @param directory The directory to search for files.
 * @param extension The file extension to filter by.
 * @return A list of files with the given extension within the specified directory and its subdirectories.
 */
fun File.getSourceFiles(extension: String): List<File> {
    return walkTopDown()
        .filter { it.isFile && it.extension == extension }
        .toList()
}

/**
 * Returns a list of files in the system classpath.
 *
 * @return A list of files in the system classpath.
 */
fun getSystemClasspath(): List<File> {
    return FileUtil.classpathDir.listFiles()?.toList() ?: emptyList()
}
