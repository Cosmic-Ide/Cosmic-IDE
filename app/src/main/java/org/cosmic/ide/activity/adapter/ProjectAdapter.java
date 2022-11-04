package org.cosmic.ide.activity.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import org.cosmic.ide.R;
import org.cosmic.ide.project.Project;

import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<ProjectAdapter.ViewHolder> {

    public interface OnProjectEventListener {
        void onProjectClicked(Project project);
        boolean onProjectLongClicked(Project project);
    }

    private final List<Project> mProjects = new ArrayList<>();
    private OnProjectEventListener onProjectEventListener;

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
        var root =
                LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.project_item, parent, false);
        final var holder = new ViewHolder(root);
        return holder;
    }

    @NonNull
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(mProjects.get(position));
        holder.background.setOnClickListener(
                v -> {
                    if (onProjectEventListener != null && position != RecyclerView.NO_POSITION) {
                        onProjectEventListener.onProjectClicked(mProjects.get(position));
                    }
                });
        holder.background.setOnLongClickListener(
                v -> {
                    if (onProjectEventListener != null
                            && position != RecyclerView.NO_POSITION) {
                        return onProjectEventListener.onProjectLongClicked(mProjects.get(position));
                    }
                    return false;
                });
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView path;
        public final View background;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.project_title);
            path = view.findViewById(R.id.project_path);
            background = view.findViewById(R.id.background);
        }

        public void bind(Project project) {
            title.setText(project.getProjectName());
            path.setText(project.getProjectDirPath());
        }
    }
}
