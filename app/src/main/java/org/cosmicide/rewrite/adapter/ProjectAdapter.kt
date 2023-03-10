package org.cosmicide.rewrite.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmicide.project.Project
import org.cosmicide.rewrite.databinding.ProjectItemBinding
import kotlin.properties.Delegates

class ProjectAdapter(private val listener: OnProjectEventListener) : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {
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

    interface OnProjectEventListener {
        fun onProjectClicked(project: Project)
        fun onProjectLongClicked(project: Project): Boolean
    }

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

    class ViewHolder(
        private val binding: ProjectItemBinding,
        private val listener: OnProjectEventListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.projectTitle.text = project.name
            binding.projectPath.text = project.root.absolutePath
            binding.root.setOnClickListener { listener.onProjectClicked(project) }
            binding.root.setOnLongClickListener { listener.onProjectLongClicked(project) }
        }
    }
}