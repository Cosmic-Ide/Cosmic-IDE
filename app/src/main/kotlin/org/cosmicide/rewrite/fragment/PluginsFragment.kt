/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.view.View
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.cosmicide.rewrite.adapter.PluginAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentPluginsBinding
import org.cosmicide.rewrite.databinding.PluginInfoBinding
import org.cosmicide.rewrite.plugin.api.Plugin
import org.cosmicide.rewrite.plugin.api.PluginLoader
import org.cosmicide.rewrite.util.CommonUtils
import org.cosmicide.rewrite.util.FileUtil

class PluginsFragment : BaseBindingFragment<FragmentPluginsBinding>() {

    override fun getViewBinding() = FragmentPluginsBinding.inflate(layoutInflater)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.pluginsList.apply {
            adapter = PluginAdapter(object : PluginAdapter.OnPluginEventListener {
                override fun onPluginClicked(plugin: Plugin) {
                    val dialog = BottomSheetDialog(requireActivity())
                    val binding = PluginInfoBinding.inflate(layoutInflater)
                    val bottomSheetView = binding.root
                    binding.apply {
                        val title = "${plugin.name} v${plugin.version}"
                        pluginName.text = title
                        val desc = plugin.description
                            .ifEmpty { "No description" } + "\n\n" + plugin.source
                        CommonUtils.getMarkwon().setMarkdown(pluginDescription, desc)
                    }
                    dialog.setContentView(bottomSheetView)
                    dialog.show()
                }

                override fun onPluginLongClicked(plugin: Plugin) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete plugin")
                        .setMessage("Are you sure you want to delete ${plugin.name}?")
                        .setPositiveButton("Yes") { _, _ ->
                            FileUtil.pluginDir.resolve(plugin.name).deleteRecursively()
                            (adapter as PluginAdapter).submitList(getPlugins())
                        }
                        .setNegativeButton("No") { _, _ -> }
                        .show()
                }

                override fun onEnablePlugin(plugin: Plugin, enabled: Boolean) {
                    val config = FileUtil.pluginDir.resolve(plugin.name).resolve("config.json")
                    val data = Gson().fromJson<MutableMap<String, String>>(
                        config.readText(),
                        object : TypeToken<Map<String, String>>() {}.type
                    )
                    data["enabled"] = enabled.toString()
                    config.writeText(Gson().toJson(data))

                    if (enabled) {
                        CommonUtils.showSnackBar(binding.root, "Loading ${plugin.name}...")
                        PluginLoader.loadPlugin(FileUtil.pluginDir.resolve(plugin.name), plugin)
                    } else {
                        CommonUtils.showSnackBar(
                            binding.root,
                            "Please restart the app to disable ${plugin.name}"
                        )
                    }
                }
            })
            (adapter as PluginAdapter).submitList(getPlugins())
        }
    }

    companion object {
        fun getPlugins(): List<Plugin> {
            val plugins = mutableListOf<Plugin>()
            FileUtil.pluginDir.listFiles { file -> file.isDirectory }?.forEach { file ->
                val config = file.resolve("config.json")
                if (config.exists()) {
                    val data =
                        Gson().fromJson<Map<String, String>>(
                            config.readText(),
                            object : TypeToken<Map<String, String>>() {}.type
                        )
                    plugins.add(Plugin(
                        name = data.getOrDefault("name", file.name),
                        version = data.getOrDefault("version", "1.0.0"),
                        author = data.getOrDefault("author", "Unknown"),
                        description = data.getOrDefault("description", ""),
                        source = data.getOrDefault("source", "Unknown"),
                        isEnabled = data.getOrDefault("enabled", "false").toBoolean()
                    ))
                }
            }
            return plugins
        }
    }
}