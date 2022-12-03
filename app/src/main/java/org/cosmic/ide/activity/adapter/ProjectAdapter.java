package org.cosmic.ide.activity.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.cosmic.ide.databinding.ProjectItemBinding;
import org.cosmic.ide.project.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    public interface OnProjectEventListener {
        void onProjectClicked(Project project);

        boolean onProjectLongClicked(Project project);
    }

    private final List<Project> mProjects = new ArrayList<>();
    private static OnProjectEventListener onProjectEventListener;

    public void setOnProjectEventListener(OnProjectEventListener onProjectEventListener) {
        this.onProjectEventListener = onProjectEventListener;
    }

    public void submitList(@NonNull List<Project> projects) {
        var diffResult =
                DiffUtil.calculateDiff(
                        new DiffUtil.Callback() {
                            @Override
                            public int getOldListSize() {
                                return mProjects.size();
                            }

                            @Override
                            public int getNewListSize() {
                                return projects.size();
                            }

                            @Override
                            public boolean areItemsTheSame(
                                    int oldItemPosition, int newItemPosition) {
                                return mProjects
                                        .get(oldItemPosition)
                                        .equals(projects.get(newItemPosition));
                            }

                            @Override
                            public boolean areContentsTheSame(
                                    int oldItemPosition, int newItemPosition) {
                                return mProjects
                                        .get(oldItemPosition)
                                        .equals(projects.get(newItemPosition));
                            }
                        });
        mProjects.clear();
        mProjects.addAll(projects);
        diffResult.dispatchUpdatesTo(this);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final var binding =
                ProjectItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        final var holder = new ViewHolder(binding);
        return holder;
    }

    @NonNull
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(mProjects.get(position));
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private ProjectItemBinding binding;

        public ViewHolder(ProjectItemBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(@NonNull Project project) {
            binding.projectTitle.setText(project.getProjectName());
            binding.projectPath.setText(project.getProjectDirPath());

            binding.getRoot().setOnClickListener(v -> {
                onProjectEventListener.onProjectClicked(project);
            });
            binding.getRoot().setOnLongClickListener(v -> {
                return onProjectEventListener.onProjectLongClicked(project);
            });
        }
    }
}
