package org.cosmic.ide.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmic.ide.databinding.ProjectItemBinding
import org.cosmic.ide.project.Project
import java.io.File

class ProjectsAdapter(private val listener: OnProjectEventListener) :
    RecyclerView.Adapter<ProjectsViewHolder>() {
    private var projects = listOf<Project>()

    fun submitList(newProjects: List<Project>) {
        if (newProjects != projects) {
            projects = newProjects
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ProjectsViewHolder.from(parent)

    override fun onBindViewHolder(holder: ProjectsViewHolder, position: Int) =
        holder.bind(projects.get(position), listener)

    override fun getItemCount() = projects.size
}

interface OnProjectEventListener {
    fun onProjectClicked(root: File)
    fun onProjectLongClicked(project: Project): Boolean
}

class ProjectsViewHolder private constructor(private val binding: ProjectItemBinding) :
    RecyclerView.ViewHolder(binding.root) {

    fun bind(project: Project, listener: OnProjectEventListener) {
        binding.projectTitle.text = project.projectName
        binding.projectPath.text = project.projectDirPath
        binding.root.setOnClickListener { listener.onProjectClicked(project.rootFile) }
        binding.root.setOnLongClickListener { listener.onProjectLongClicked(project) }
    }

    companion object {
        fun from(parent: ViewGroup) =
            ProjectsViewHolder(ProjectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }
}