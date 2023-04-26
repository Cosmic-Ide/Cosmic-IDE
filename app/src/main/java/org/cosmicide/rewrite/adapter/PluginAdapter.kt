package org.cosmicide.rewrite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmicide.project.Project
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
     * List of [Project] objects to be displayed in the [RecyclerView].
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
     * Sets the list of projects to be displayed in the [RecyclerView].
     *
     * @param projects The list of [Project] objects.
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
     * ViewHolder for displaying a single [Project] object in the [RecyclerView].
     *
     * @param binding The [PluginItemBinding] object for the item view.
     * @param listener The [OnProjectEventListener] for handling user interactions with the item view.
     */
    class ViewHolder(
        private val binding: PluginItemBinding,
        private val listener: OnPluginEventListener
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the given [Project] object to the item view.
         *
         * @param plugin The [Project] object to be displayed.
         */
        fun bind(plugin: Plugin) {
            val title = "${plugin.getName()} v${plugin.getVersion()}"
            binding.title.text = title
            binding.author.text = plugin.getAuthor()
            binding.root.setOnClickListener { listener.onPluginClicked(plugin) }
        }
    }
}
