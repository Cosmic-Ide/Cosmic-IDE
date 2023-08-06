/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */


package changedatadirectory

import android.widget.Toast
import de.Maxr1998.modernpreferences.helpers.onClick
import de.Maxr1998.modernpreferences.preferences.EditTextPreference
import org.cosmicide.rewrite.plugin.api.HookManager
import org.cosmicide.rewrite.plugin.api.PluginLoader
import org.cosmicide.rewrite.util.FileUtil
import java.io.File

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        PluginLoader.registerPreferences(EditTextPreference("data_directory").apply {
            title = "Data directory"
            summary = "The directory where Cosmic IDE stores its data"
            onClick {
                val dir = File(currentInput.toString())
                if (currentInput.isNullOrBlank()) {
                    Toast.makeText(
                        HookManager.context.get(),
                        "Please provide a proper directory",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@onClick false
                }
                if (dir.exists().not()) {
                    Toast.makeText(
                        HookManager.context.get(),
                        "The specified directory does not exist",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@onClick false
                }
                updateDirectory(dir)
                true
            }
        })

    }

    private fun updateDirectory(dir: File) {
        if (dir.exists().not()) {
            dir.mkdirs()
        }
        FileUtil.dataDir = dir
    }
}
