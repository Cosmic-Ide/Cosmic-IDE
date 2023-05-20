/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmicide.rewrite.databinding.PluginItemBinding
import org.cosmicide.rewrite.plugin.api.Plugin
import kotlin.properties.Delegates

/**
 * Adapter for displaying a list of [Plugin] objects in a [RecyclerView].
 *
 * @param listener Listener for handling user interactions with the items in the list.
 */
class PluginAdapter(private val listener: OnPluginEventListener) :
    RecyclerView.Adapter<PluginAdapter.ViewHolder>() {

    /**
     * List of [Plugin] objects to be displayed in the [RecyclerView].
     */
    private var plugins: List<Plugin> by Delegates.observable(emptyList()) { _, oldList, newList ->
        DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = oldList.size
            override fun getNewListSize() = newList.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition] == newList[newItemPosition]

            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition] == newList[newItemPosition]
        }).dispatchUpdatesTo(this)
    }

    /**
     * Interface for handling user interactions with the items in the list.
     */
    interface OnPluginEventListener {
        fun onPluginClicked(plugin: Plugin)
    }

    /**
     * Sets the list of plugins to be displayed in the [RecyclerView].
     *
     * @param projects The list of [Plugin] objects.
     */
    fun submitList(plugins: List<Plugin>) {
        this.plugins = plugins
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            PluginItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(plugins[position])
    }

    override fun getItemCount() = plugins.size

    /**
     * ViewHolder for displaying a single [Plugin] object in the [RecyclerView].
     *
     * @param binding The [PluginItemBinding] object for the item view.
     * @param listener The [OnProjectEventListener] for handling user interactions with the item view.
     */
    class ViewHolder(
        private val binding: PluginItemBinding,
        private val listener: OnPluginEventListener
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the given [Plugin] object to the item view.
         *
         * @param plugin The [Plugin] object to be displayed.
         */
        fun bind(plugin: Plugin) {
            val title = "${plugin.getName()} v${plugin.getVersion()}"
            binding.title.text = title
            binding.author.text = plugin.getAuthor()
            binding.root.setOnClickListener { listener.onPluginClicked(plugin) }
        }
    }
}
