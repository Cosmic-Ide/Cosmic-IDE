/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.adapter
/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmicide.databinding.NewPluginItemBinding
import org.cosmicide.rewrite.plugin.api.Plugin
import kotlin.properties.Delegates

/**
 * Adapter for displaying a list of [Plugin] objects in a [RecyclerView].
 *
 * @param listener Listener for handling user interactions with the items in the list.
 */
class AvailablePluginAdapter(private val listener: OnPluginEventListener) :
    RecyclerView.Adapter<AvailablePluginAdapter.ViewHolder>() {

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

        fun onPluginLongClicked(plugin: Plugin)

        fun onPluginInstall(plugin: Plugin)
    }

    /**
     * Sets the list of plugins to be displayed in the [RecyclerView].
     *
     * @param plugins The list of [Plugin] objects.
     */
    fun submitList(plugins: List<Plugin>) {
        this.plugins = plugins
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            NewPluginItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(plugins[position])
    }

    override fun getItemCount() = plugins.size

    /**
     * ViewHolder for displaying a single [Plugin] object in the [RecyclerView].
     *
     * @param itemBinding The [NewPluginItemBinding] object for the item view.
     * @param listener The [OnPluginEventListener] for handling user interactions with the item view.
     */
    inner class ViewHolder(
        itemBinding: NewPluginItemBinding,
        private val listener: OnPluginEventListener
    ) : BindableViewHolder<Plugin, NewPluginItemBinding>(itemBinding) {

        override fun bind(data: Plugin) {
            val title = "${data.name} ${data.version}"
            binding.apply {
                name.text = title
                author.text = data.author
                button.setOnClickListener { listener.onPluginInstall(data) }
                root.setOnClickListener { listener.onPluginClicked(data) }
                root.setOnLongClickListener {
                    listener.onPluginLongClicked(data)
                    true
                }
            }
        }
    }
}
