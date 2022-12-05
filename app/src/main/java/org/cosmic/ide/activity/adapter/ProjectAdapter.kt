package org.cosmic.ide.activity.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.cosmic.ide.databinding.ProjectItemBinding
import org.cosmic.ide.project.Project

class ProjectAdapter : RecyclerView.Adapter<ProjectAdapter.ViewHolder?>() {
    private val mProjects: MutableList<Project> = ArrayList()

    interface OnProjectEventListener {
        fun onProjectClicked(project: Project?)
        fun onProjectLongClicked(project: Project?): Boolean
    }

    fun setOnProjectEventListener(onProjectEventListener: OnProjectEventListener?) {
        Companion.onProjectEventListener = onProjectEventListener
    }

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
                    oldItemPosition: Int, newItemPosition: Int
                ): Boolean {
                    return (mProjects[oldItemPosition]
                            == projects[newItemPosition])
                }

                override fun areContentsTheSame(
                    oldItemPosition: Int, newItemPosition: Int
                ): Boolean {
                    return (mProjects[oldItemPosition]
                            == projects[newItemPosition])
                }
            })
        mProjects.clear()
        mProjects.addAll(projects)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding: ProjectItemBinding =
            ProjectItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(mProjects[position])
    }

    override fun getItemCount(): Int {
        return mProjects.size
    }

    class ViewHolder(private val binding: ProjectItemBinding) : RecyclerView.ViewHolder(
        binding.root
    ) {
        fun bind(project: Project) {
            binding.projectTitle.text = project.projectName
            binding.projectPath.text = project.projectDirPath
            binding.root.setOnClickListener { v ->
                onProjectEventListener!!.onProjectClicked(
                    project
                )
            }
            binding.root.setOnLongClickListener { v ->
                onProjectEventListener!!.onProjectLongClicked(
                    project
                )
            }
        }
    }

    companion object {
        private var onProjectEventListener: OnProjectEventListener? = null
    }
}