/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

import org.cosmicide.rewrite.util.FileUtil

object ResourceUtil {

    val resources =
        arrayOf("classpath/android.jar", "index.json", "classpath/core-lambda-stubs.jar")

    fun missingResources(): List<String> {
        val missing = mutableListOf<String>()
        for (resource in resources) {
            val file = FileUtil.dataDir.resolve(resource)
            if (!file.exists()) {
                missing.add(resource)
            }
        }
        return missing
    }
}
