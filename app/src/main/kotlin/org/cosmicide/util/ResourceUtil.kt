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
        return !isRuntimeReady(context!!.filesDir.resolve("glibc"))
    }

    fun isRuntimeReady(directory: File): Boolean {
        return directory.resolve(".installed").isFile &&
            directory.resolve("usr/bin/bash").canExecute() &&
            directory.resolve("usr/lib/libc.so.6").isFile
    }

    fun isBootstrapIncomplete(): Boolean {
        return isRuntimeMissing()
    }

    fun prepareLanguageServerSetupScript(): File {
        val context = checkNotNull(App.instance.get()) { "Application context is unavailable" }
        val script = context.filesDir.resolve("setup.sh")
        context.assets.open("setup.sh").use { input ->
            script.outputStream().use { output -> input.copyTo(output) }
        }
        val packageInstaller = context.filesDir.resolve("alarm-pkg")
        context.assets.open("alarm-pkg").use { input ->
            packageInstaller.outputStream().use { input.copyTo(it) }
        }
        check(packageInstaller.setExecutable(true)) { "Cannot make package installer executable." }
        check(script.setExecutable(true)) { "Cannot make setup script executable." }
        return script
    }

    /**
     * Determines whether any essential item (standard resource or toolchain component)
     * is missing, signifying that setup is required.
     */
    fun isEnvironmentIncomplete(): Boolean {
        val directory = checkNotNull(App.instance.get()).filesDir.resolve("arch")
        return isBootstrapIncomplete() ||
            !listOf("bash", "git", "pacman", "java").all {
                directory.resolve("usr/bin/$it").canExecute()
            }
    }
}
