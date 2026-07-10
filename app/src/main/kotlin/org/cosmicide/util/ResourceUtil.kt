/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

import org.cosmicide.App
import java.io.File

object ResourceUtil {

    val resources =
        arrayOf<String>(
        )

    fun missingResources(): List<String> {
        val missing = mutableListOf<String>()
        for (resource in resources) {
            val file = FileUtil.dataDir.resolve(resource)
            if (!file.exists()) {
                if (!App.instance.get()!!.filesDir.resolve(resource).exists())
                    missing.add(resource)
            }
        }
        return missing
    }

    fun isRuntimeMissing(): Boolean {
        val context = App.instance.get()
        return !context!!.filesDir.resolve("glibc").exists()
    }

    fun isJdkMissing(): Boolean {
        val context = App.instance.get()
        return context!!.jdks().isEmpty()
    }

    fun isKotlinMissing(): Boolean {
        val kotlinTargetDir = File(FileUtil.dataDir, "kotlinc")
        return !kotlinTargetDir.exists() || kotlinTargetDir.listFiles()?.isEmpty() == true
    }

    fun isJdtlsMissing(): Boolean {
        val context = App.instance.get() ?: return true
        val jdtlsTargetDir = File(context.filesDir, "jdtls")
        return !jdtlsTargetDir.exists() || jdtlsTargetDir.listFiles()?.isEmpty() == true
    }

    /**
     * Determines whether any essential item (standard resource or toolchain component)
     * is missing, signifying that setup is required.
     */
    fun isEnvironmentIncomplete(): Boolean {
        return missingResources().isNotEmpty() ||
                isRuntimeMissing() ||
                isJdkMissing() ||
                isKotlinMissing() ||
                isJdtlsMissing()
    }
}
