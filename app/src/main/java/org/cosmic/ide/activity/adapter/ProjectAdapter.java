package org.cosmic.ide.activity.adapter;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.lists.TwoLineItemViewHolder;
import org.cosmic.ide.project.Project;
import java.util.ArrayList;
import java.util.List;

public class ProjectAdapter extends RecyclerView.Adapter<TwoLineItemViewHolder> {

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
    public TwoLineItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        final var holder = new TwoLineItemViewHolder.create(parent);

        return holder;
    }

    @NonNull
    @Override
    public void onBindViewHolder(TwoLineItemViewHolder holder, int position) {
        var project = mProjects.get(position);

        holder.text.setText(project.getProjectName());
        holder.secondary.setText(project.getProjectDirPath());

        holder.itemView.setOnClickListener(v -> {
            onProjectEventListener.onProjectClicked(project);
        });
        holder.itemView.setOnLongClickListener(v -> {
            return onProjectEventListener.onProjectLongClicked(project);
        });
    }

    @Override
    public int getItemCount() {
        return mProjects.size();
    }
}