/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.fragment

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.noties.markwon.Markwon
import io.noties.markwon.linkify.LinkifyPlugin
import org.cosmicide.rewrite.adapter.PluginAdapter
import org.cosmicide.rewrite.common.BaseBindingFragment
import org.cosmicide.rewrite.databinding.FragmentPluginsBinding
import org.cosmicide.rewrite.databinding.PluginInfoBinding
import org.cosmicide.rewrite.plugin.api.Plugin
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
                        val title = "${plugin.getName()} v${plugin.getVersion()}"
                        pluginName.text = title
                        val markwon = Markwon.builder(context)
                            .usePlugin(LinkifyPlugin.create())
                            .build()
                        val desc = plugin.getDescription()
                            .ifEmpty { "No description" } + "\n\n" + plugin.getSource()
                        pluginDescription.movementMethod = LinkMovementMethod.getInstance()
                        pluginDescription.text = markwon.toMarkdown(desc)
                    }
                    dialog.setContentView(bottomSheetView)
                    dialog.show()
                }

                override fun onPluginLongClicked(plugin: Plugin) {
                    MaterialAlertDialogBuilder(requireContext())
                        .setTitle("Delete plugin")
                        .setMessage("Are you sure you want to delete ${plugin.getName()}?")
                        .setPositiveButton("Yes") { _, _ ->
                            FileUtil.pluginDir.resolve(plugin.getName()).deleteRecursively()
                            (adapter as PluginAdapter).submitList(getPlugins())
                        }
                        .setNegativeButton("No") { _, _ -> }
                        .show()
                }
            })
            (adapter as PluginAdapter).submitList(getPlugins())
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    companion object {
        fun getPlugins(): List<Plugin> {
            val plugins = mutableListOf<Plugin>()
            FileUtil.pluginDir.listFiles { file -> file.isDirectory }?.forEach { file ->
                val config = file.resolve("config.json")
                config.writeText(
                    """
{
"name":"Mama",
"version":"1.0.0",
"author":"Pranav",
"description": "Pro Plugin",
"source": "earth"
}
                        """
                )
                if (config.exists()) {
                    val data =
                        Gson().fromJson<Map<String, String>>(
                            config.readText(),
                            object : TypeToken<Map<String, String>>() {}.type
                        )
                    plugins.add(object : Plugin {
                        override fun getName(): String {
                            return data.getValue("name")
                        }

                        override fun getVersion(): String {
                            return data.getValue("version")
                        }

                        override fun getAuthor(): String {
                            return data.getValue("author")
                        }

                        override fun getDescription(): String {
                            return data.getValue("description")
                        }

                        override fun getSource(): String {
                            return data.getValue("source")
                        }
                    })
                }
            }
            return plugins
        }
    }
}