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

    fun isRuntimeMissing(): Boolean {
        val context = App.instance.get()
        return !context!!.filesDir.resolve("glibc").exists()
    }

    fun isJdkMissing(): Boolean {
        val context = App.instance.get()
        return context!!.jdks().isEmpty()
    }
    fun isGradleGroovyLspMissing(): Boolean {
        val context = App.instance.get() ?: return true
        return !context.filesDir
            .resolve(
                "vscode-gradle/gradle-language-server/build/install/" +
                        "gradle-language-server/bin/gradle-language-server"
            )
            .isFile
    }

    fun isBootstrapIncomplete(): Boolean {
        return isRuntimeMissing()
    }

    fun isLanguageServerSetupIncomplete(): Boolean {
        return isKotlinLspMissing()
//                isGradleGroovyLspMissing()
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
