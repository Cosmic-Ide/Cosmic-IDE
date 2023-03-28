package org.cosmicide.rewrite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmicide.project.Project
import org.cosmicide.rewrite.databinding.ProjectItemBinding
import kotlin.properties.Delegates

/**
 * Adapter for displaying a list of [Project] objects in a [RecyclerView].
 *
 * @param listener Listener for handling user interactions with the items in the list.
 */
class ProjectAdapter(private val listener: OnProjectEventListener) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

    /**
     * List of [Project] objects to be displayed in the [RecyclerView].
     */
    private var projects: List<Project> by Delegates.observable(emptyList()) { _, oldList, newList ->
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
    interface OnProjectEventListener {
        fun onProjectClicked(project: Project)
        fun onProjectLongClicked(project: Project): Boolean
    }

    /**
     * Sets the list of projects to be displayed in the [RecyclerView].
     *
     * @param projects The list of [Project] objects.
     */
    fun submitList(projects: List<Project>) {
        this.projects = projects
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(
            ProjectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false),
            listener
        )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(projects[position])
    }

    override fun getItemCount() = projects.size

    /**
     * ViewHolder for displaying a single [Project] object in the [RecyclerView].
     *
     * @param binding The [ProjectItemBinding] object for the item view.
     * @param listener The [OnProjectEventListener] for handling user interactions with the item view.
     */
    class ViewHolder(
        private val binding: ProjectItemBinding,
        private val listener: OnProjectEventListener
    ) : RecyclerView.ViewHolder(binding.root) {

        /**
         * Binds the given [Project] object to the item view.
         *
         * @param project The [Project] object to be displayed.
         */
        fun bind(project: Project) {
            binding.projectTitle.text = project.name
            binding.projectPath.text = project.root.absolutePath
            binding.root.setOnClickListener { listener.onProjectClicked(project) }
            binding.root.setOnLongClickListener { listener.onProjectLongClicked(project) }
        }
    }
}