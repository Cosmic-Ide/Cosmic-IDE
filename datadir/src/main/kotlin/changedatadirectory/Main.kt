/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */


package changedatadirectory

import android.util.Log
import android.widget.Toast
import de.Maxr1998.modernpreferences.PreferenceScreen
import de.Maxr1998.modernpreferences.helpers.editText
import de.Maxr1998.modernpreferences.preferences.EditTextPreference
import org.cosmicide.rewrite.plugin.api.HookManager
import org.cosmicide.rewrite.util.FileUtil
import java.io.File

object Main {

    private val pref = HookManager.context.get()!!.getSharedPreferences("datadir", 0)

    @JvmStatic
    fun main(args: Array<String>) {
        val context = HookManager.context.get()!!
        val dataDir = pref.getString("data_directory", null)

        if (dataDir == null) {
            Log.d("Plugin", "Data directory not set")
            return
        }

        val dir = File(dataDir)

        val isGranted = dir.canWrite()

        if (isGranted.not()) {
            Toast.makeText(
                context,
                "Couldn't change data dir due to missing storage permission, check settings to grant it.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        if (dir.exists()) {
            Log.d("Plugin", "Data directory already set to $dataDir")
            updateDirectory(dir)
        }
        Log.d("Plugin", "Loaded plugin ChangeDataDirectory")
    }

    private fun updateDirectory(dir: File) {
        val oldDir = FileUtil.dataDir
        Log.d("Plugin", "Updating data directory to $dir")
        dir.mkdirs()
        FileUtil.init(dir)
        // Copy plugins from old directory
        Toast.makeText(
            HookManager.context.get(),
            "Copying data from old directory...",
            Toast.LENGTH_SHORT
        ).show()
        oldDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.copyTo(FileUtil.dataDir.resolve(file.name), overwrite = true)
            } else {
                file.copyRecursively(FileUtil.dataDir.resolve(file.name), overwrite = true)
            }
        }
        Log.d(
            "Plugin",
            "dataDir: ${FileUtil.dataDir.exists()} classpath: ${FileUtil.classpathDir.exists()} project: ${FileUtil.projectDir.exists()}"
        )
    }

    @JvmStatic
    fun registerPreferences(builder: PreferenceScreen.Builder) {
        val context = HookManager.context.get()!!

        builder.apply {
            editText("data_directory") {
                title = "Data directory"
                summary = "Current: ${FileUtil.dataDir}"
                defaultValue = FileUtil.dataDir.absolutePath

                textChangeListener = EditTextPreference.OnTextChangeListener { _, text ->
                    val dir = text.toString()
                    val file = File(dir)
                    if (file.isFile) {
                        Toast.makeText(
                            context,
                            "Data directory must be a directory",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@OnTextChangeListener false
                    }
                    if (file.mkdirs().not()) {
                        Toast.makeText(context, "Couldn't create directory", Toast.LENGTH_SHORT)
                            .show()
                        return@OnTextChangeListener false
                    }
                    pref.edit().putString("data_directory", text.toString()).apply()
                    updateDirectory(file)
                    true
                }
            }
        }
    }
}
