package org.cosmic.ide.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmic.ide.databinding.ProjectItemBinding
import org.cosmic.ide.project.Project
import java.io.File

class ProjectsAdapter : RecyclerView.Adapter<ProjectsAdapter.ViewHolder>() {
    private val mProjects = mutableListOf<Project>()

    interface OnProjectEventListener {
        fun onProjectClicked(root: File)
        fun onProjectLongClicked(project: Project): Boolean
    }

    lateinit var onProjectEventListener: OnProjectEventListener

    fun submitList(projects: List<Project>) {
        val diffResult = DiffUtil.calculateDiff(
            object : DiffUtil.Callback() {
                override fun getOldListSize(): Int {
                    return mProjects.size
                }

                override fun getNewListSize(): Int {
                    return projects.size
                }

                override fun areItemsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return (
                        mProjects.get(oldItemPosition)
                            == projects.get(newItemPosition)
                        )
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int,
                    newItemPosition: Int
                ): Boolean {
                    return (
                        mProjects.get(oldItemPosition)
                            == projects.get(newItemPosition)
                        )
                }
            })
        mProjects.clear()
        mProjects.addAll(projects)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ProjectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), onProjectEventListener)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mProjects.get(position))
    }

    override fun getItemCount() = mProjects.size

    inner class ViewHolder(
        private val binding: ProjectItemBinding,
        val listener: OnProjectEventListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.projectTitle.text = project.projectName
            binding.projectPath.text = project.projectDirPath
            binding.root.setOnClickListener {
                listener.onProjectClicked(project.rootFile)
            }
            binding.root.setOnLongClickListener {
                listener.onProjectLongClicked(project)
            }
        }
    }
}