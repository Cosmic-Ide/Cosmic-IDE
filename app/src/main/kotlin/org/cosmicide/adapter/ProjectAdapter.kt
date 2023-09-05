/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmicide.databinding.ProjectItemBinding
import org.cosmicide.project.Project
import kotlin.properties.Delegates

/**
 * Adapter for displaying a list of [Project] objects in a [RecyclerView].
 *
 * @param listener Listener for handling user interactions with the items in the list.
 */
class ProjectAdapter(private val listener: OnProjectEventListener) :
    RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {

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
        fun onProjectLongClicked(project: Project, v: View): Boolean
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
     * @param itemBinding The [ProjectItemBinding] object for the item view.
     * @param listener The [OnProjectEventListener] for handling user interactions with the item view.
     */
    inner class ViewHolder(
        itemBinding: ProjectItemBinding,
        private val listener: OnProjectEventListener
    ) : BindableViewHolder<Project, ProjectItemBinding>(itemBinding) {

        override fun bind(data: Project) {
            binding.projectTitle.text = data.name
            binding.projectPath.text = data.root.absolutePath
            binding.root.setOnClickListener { listener.onProjectClicked(data) }
            binding.root.setOnLongClickListener {
                listener.onProjectLongClicked(
                    data,
                    binding.root
                )
            }
        }
    }
}