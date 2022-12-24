package org.cosmic.ide.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmic.ide.databinding.ProjectItemBinding
import org.cosmic.ide.project.Project

class ProjectAdapter : RecyclerView.Adapter<ProjectAdapter.ViewHolder>() {
    private val mProjects = mutableListOf<Project>()

    interface OnProjectEventListener {
        fun onProjectClicked(project: Project)
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

    class ViewHolder(
        private val binding: ProjectItemBinding,
        val listener: OnProjectEventListener
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(project: Project) {
            binding.projectTitle.text = project.projectName
            binding.projectPath.text = project.projectDirPath
            binding.root.setOnClickListener { _ ->
                listener.onProjectClicked(
                    project
                )
            }
            binding.root.setOnLongClickListener { _ ->
                listener.onProjectLongClicked(
                    project
                )
            }
        }
    }
}