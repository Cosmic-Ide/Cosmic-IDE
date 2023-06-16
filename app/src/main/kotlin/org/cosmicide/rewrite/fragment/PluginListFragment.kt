/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment
/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.rewrite.adapter.AvailablePluginAdapter
import org.cosmicide.rewrite.adapter.PluginAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.databinding.FragmentPluginListBinding
import org.cosmicide.rewrite.databinding.PluginInfoBinding
import org.cosmicide.rewrite.plugin.api.Plugin
import org.cosmicide.rewrite.util.FileUtil
import java.net.URL

class PluginListFragment : BaseBindingFragment<FragmentPluginListBinding>() {

    override fun getViewBinding() = FragmentPluginListBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.pluginsList.apply {
            adapter = AvailablePluginAdapter(object : AvailablePluginAdapter.OnPluginEventListener {
                override fun onPluginClicked(plugin: Plugin) {
                    val dialog = BottomSheetDialog(requireActivity())
                    val binding = PluginInfoBinding.inflate(layoutInflater)
                    val bottomSheetView = binding.root
                    binding.apply {
                        val title = "${plugin.name} v${plugin.version}"
                        pluginName.text = title
                        val markwon = Markwon.builder(context)
                            .usePlugin(LinkifyPlugin.create())
                            .build()
                        val desc = plugin.description
                            .ifEmpty { "No description" } + "\n\n" + plugin.source
                        pluginDescription.movementMethod = LinkMovementMethod.getInstance()
                        pluginDescription.text = markwon.toMarkdown(desc)
                    }
                    dialog.setContentView(bottomSheetView)
                    dialog.show()
                }

                override fun onPluginLongClicked(plugin: Plugin) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete plugin")
                        .setMessage("Are you sure you want to delete ${plugin.name}?")
                        .setPositiveButton("Yes") { _, _ ->
                            lifecycleScope.launch {
                                FileUtil.pluginDir.resolve(plugin.name).deleteRecursively()
                                val plugins = getPlugins()
                                (adapter as PluginAdapter).submitList(plugins)
                            }
                        }
                        .setNegativeButton("No") { _, _ -> }
                        .show()
                }

                override fun onPluginSelected(plugin: Plugin) {
                    Snackbar.make(
                        binding.root,
                        "Installing ${plugin.name}...",
                        Snackbar.LENGTH_SHORT
                    ).show()
                    lifecycleScope.launch(Dispatchers.IO) {
                        val pluginFolder = FileUtil.pluginDir.resolve(plugin.name)
                        pluginFolder.mkdirs()
                        pluginFolder.resolve(plugin.name).resolve("config.json").writeText(
                            """
                        {
                            "name":" ${plugin.name}",
                            "version": "${plugin.version}",
                            "author":" ${plugin.author}",
                            "description": "${plugin.description}",
                            "source": "${plugin.source}"
                        }
                        """.trimMargin()
                        )
                        pluginFolder.resolve("classes.dex").apply {
                            if (!exists()) {
                                createNewFile()
                            }
                            writeBytes(URL("${plugin.source}/releases/download/${plugin.version}/classes.dex").readBytes())
                        }
                        pluginFolder.resolve("config.json").writeText(plugin.raw)
                        lifecycleScope.launch(Dispatchers.Main) {
                            Snackbar.make(
                                binding.root,
                                "${plugin.name} installed",
                                Snackbar.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                }
            })
            lifecycleScope.launch {
                (adapter as AvailablePluginAdapter).submitList(getPlugins())
            }
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    suspend fun getPlugins(): List<Plugin> {
        return withContext(Dispatchers.IO) {
            try {
                val pluginsJson = URL(Prefs.pluginRepository).readText()
                val pluginsType = object : TypeToken<List<Plugin>>() {}.type
                Gson().fromJson(pluginsJson, pluginsType) as List<Plugin>
            } catch (e: Exception) {
                throw IllegalArgumentException("Failed to get plugins", e)
            }
        }
    }
}