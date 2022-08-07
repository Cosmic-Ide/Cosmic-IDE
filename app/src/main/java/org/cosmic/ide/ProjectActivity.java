package org.cosmic.ide;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.cosmic.ide.adapter.ProjectAdapter;
import org.cosmic.ide.common.util.CoroutineUtil;
import org.cosmic.ide.databinding.ActivityProjectBinding;
import org.cosmic.ide.project.JavaProject;
import org.cosmic.ide.ui.utils.UiUtilsKt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ProjectActivity extends BaseActivity {

    public interface OnProjectCreatedListener {
        void onProjectCreated(JavaProject project);
    }

    private ProjectAdapter projectAdapter;
    private ActivityProjectBinding binding;

    private AlertDialog createNewProjectDialog;
    private AlertDialog deleteProjectDialog;

    private OnProjectCreatedListener mListener;

    public void setOnProjectCreatedListener(OnProjectCreatedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buildCreateNewProjectDialog();
        buildDeleteProjectDialog();

        setSupportActionBar(binding.toolbar);
        UiUtilsKt.addSystemWindowInsetToPadding(binding.projectRecycler, false, false, false, true);
        UiUtilsKt.addSystemWindowInsetToPadding(binding.appbar, false, true, false, false);
        UiUtilsKt.addSystemWindowInsetToMargin(binding.fab, false, false, false, true);

        projectAdapter = new ProjectAdapter();
        binding.projectRecycler.setAdapter(projectAdapter);
        binding.projectRecycler.setLayoutManager(new LinearLayoutManager(this));
        projectAdapter.setOnProjectSelectedListener(this::openProject);
        projectAdapter.setOnProjectLongClickedListener(this::deleteProject);
        setOnProjectCreatedListener(this::openProject);

        binding.refreshLayout.setOnRefreshListener(
                () -> {
                    loadProjects();
                    binding.refreshLayout.setRefreshing(false);
                });
        binding.fab.setOnClickListener(v -> showCreateNewProjectDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.projects_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingActivity.class));
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        binding.projectRecycler.invalidate();
        loadProjects();
    }

    @Override
    public void onDestroy() {
        if (createNewProjectDialog.isShowing()) {
            createNewProjectDialog.dismiss();
        }
        if (deleteProjectDialog.isShowing()) {
            deleteProjectDialog.dismiss();
        }
        super.onDestroy();
    }

    private void buildCreateNewProjectDialog() {
        var builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.new_project));
        builder.setView(R.layout.create_new_project_dialog);
        builder.setPositiveButton(getString(R.string.create), null);
        builder.setNegativeButton(android.R.string.cancel, null);
        createNewProjectDialog = builder.create();
    }

    private void buildDeleteProjectDialog() {
        var builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle(getString(R.string.delete_project));
        builder.setMessage("blablabla"); // DON'T REMOVE THIS LINE
        builder.setPositiveButton(getString(R.string.delete), null);
        builder.setNegativeButton(android.R.string.cancel, null);
        deleteProjectDialog = builder.create();
    }

    @WorkerThread
    private void showCreateNewProjectDialog() {
        if (!createNewProjectDialog.isShowing()) {
            createNewProjectDialog.show();
            EditText input = createNewProjectDialog.findViewById(android.R.id.text1);
            Button createBtn = createNewProjectDialog.findViewById(android.R.id.button1);
            createBtn.setOnClickListener(
                    v -> {
                        var projectName = input.getText().toString().trim();
                        if (projectName.isEmpty()) {
                            return;
                        }
                        try {
                            var project = JavaProject.newProject(projectName);
                            if (mListener != null) {
                                runOnUiThread(
                                        () -> {
                                            if (createNewProjectDialog.isShowing())
                                                createNewProjectDialog.dismiss();
                                            mListener.onProjectCreated(project);
                                        });
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
            input.setText("");
        }
    }

    private void showDeleteProjectDialog(JavaProject project) {
        if (!deleteProjectDialog.isShowing()) {
            deleteProjectDialog.show();
            TextView message = deleteProjectDialog.findViewById(android.R.id.message);
            Button deleteBtn = deleteProjectDialog.findViewById(android.R.id.button1);
            message.setText(
                    "Are you sure you want to delete the "
                            + project.getProjectName()
                            + " project?");
            deleteBtn.setOnClickListener(
                    v -> {
                        runOnUiThread(
                                () -> {
                                    if (deleteProjectDialog.isShowing())
                                        deleteProjectDialog.dismiss();
                                    project.delete();
                                    loadProjects();
                                });
                    });
        }
    }

    private void openProject(JavaProject project) {
        var projectPath = project.getProjectDirPath();
        var intent = new Intent(this, MainActivity.class);
        intent.putExtra("project_path", projectPath);
        startActivity(intent);
    }

    private boolean deleteProject(JavaProject project) {
        showDeleteProjectDialog(project);
        return true;
    }

    private void loadProjects() {
        CoroutineUtil.inParallel(
                () -> {
                    var projectDir = new File(JavaProject.getRootDirPath());
                    var directories = projectDir.listFiles(File::isDirectory);
                    var projects = new ArrayList<JavaProject>();
                    if (directories != null) {
                        Arrays.sort(directories, Comparator.comparingLong(File::lastModified));
                        for (var directory : directories) {
                            var project = new File(directory, "src");
                            if (project.exists()) {
                                var javaProject =
                                        new JavaProject(new File(directory.getAbsolutePath()));
                                projects.add(javaProject);
                            }
                        }
                    }
                    runOnUiThread(
                            () -> {
                                projectAdapter.submitList(projects);
                                toggleNullProject(projects);
                            });
                });
    }

    private void toggleNullProject(List<JavaProject> projects) {
        if (projects.size() == 0) {
            binding.projectRecycler.setVisibility(View.GONE);
            binding.emptyContainer.setVisibility(View.VISIBLE);
        } else {
            binding.projectRecycler.setVisibility(View.VISIBLE);
            binding.emptyContainer.setVisibility(View.GONE);
        }
    }
}
