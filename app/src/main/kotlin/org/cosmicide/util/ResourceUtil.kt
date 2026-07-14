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
        return !context!!.filesDir.resolve("arch").exists()
    }

    fun isJdkMissing(): Boolean {
        val context = App.instance.get()
        return context!!.jdks().isEmpty()
    }

    fun isKotlinLspMissing(): Boolean {
        val context = App.instance.get() ?: return true
        return !context.filesDir.resolve("kotlin-lsp/bin/intellij-server").isFile
    }

    fun isJdtlsMissing(): Boolean {
        val context = App.instance.get() ?: return true
        val pluginsDir = context.filesDir.resolve("jdtls/plugins")
        return pluginsDir.listFiles().orEmpty().none { file ->
            file.name.startsWith("org.eclipse.equinox.launcher_") &&
                    file.name.endsWith(".jar")
        }
    }

    fun isMetalsMissing(): Boolean {
        val context = App.instance.get() ?: return true
        return !context.filesDir.resolve("scala/bin/metals").isFile
    }

    fun isBootstrapIncomplete(): Boolean {
        return missingResources().isNotEmpty() || isRuntimeMissing()
    }

    fun isLanguageServerSetupIncomplete(): Boolean {
        return isKotlinLspMissing() || isJdtlsMissing() || isMetalsMissing()
    }

    fun prepareLanguageServerSetupScript(): File {
        val context = checkNotNull(App.instance.get()) { "Application context is unavailable" }
        val script = context.filesDir.resolve("setup.sh")
        context.assets.open("setup.sh").use { input ->
            script.outputStream().use { output -> input.copyTo(output) }
        }
        context.assets.open("alarm-pkg").use { input ->
            context.filesDir.resolve("alarm-pkg").outputStream().use { input.copyTo(it) }
        }
        script.setExecutable(true)
        return script
    }

    /**
     * Determines whether any essential item (standard resource or toolchain component)
     * is missing, signifying that setup is required.
     */
    fun isEnvironmentIncomplete(): Boolean {
        return isBootstrapIncomplete() ||
                isJdkMissing() ||
                isLanguageServerSetupIncomplete()
    }
}
