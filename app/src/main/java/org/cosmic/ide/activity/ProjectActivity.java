package org.cosmic.ide.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Switch;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.pedrovgs.lynx.LynxActivity;
import com.github.pedrovgs.lynx.LynxConfig;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;


import org.cosmic.ide.R;
import org.cosmic.ide.activity.adapter.ProjectAdapter;
import org.cosmic.ide.common.util.CoroutineUtil;
import org.cosmic.ide.databinding.ActivityProjectBinding;
import org.cosmic.ide.project.JavaProject;
import org.cosmic.ide.project.KotlinProject;
import org.cosmic.ide.project.Project;
import org.cosmic.ide.util.AndroidUtilities;
import org.cosmic.ide.util.Constants;
import org.cosmic.ide.util.UiUtilsKt;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class ProjectActivity extends BaseActivity implements ProjectAdapter.OnProjectEventListener {

    public interface OnProjectCreatedListener {
        void onProjectCreated(Project project);
    }

    private ProjectAdapter projectAdapter;
    private ActivityProjectBinding binding;

    private AlertDialog createNewProjectDialog;

    private OnProjectCreatedListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProjectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        buildCreateNewProjectDialog();

        projectAdapter = new ProjectAdapter();
        binding.projectRecycler.setAdapter(projectAdapter);
        binding.projectRecycler.setLayoutManager(new LinearLayoutManager(this));
        projectAdapter.setOnProjectEventListener(this);
        mListener = this::onProjectClicked;

        UiUtilsKt.addSystemWindowInsetToMargin(binding.fab, false, false, false, true);
        UiUtilsKt.addSystemWindowInsetToPadding(binding.appbar, false, true, false, false);
        UiUtilsKt.addSystemWindowInsetToPadding(binding.projectRecycler, false, false, false, true);

        binding.refreshLayout.setOnRefreshListener(
                () -> {
                    loadProjects();
                    binding.refreshLayout.setRefreshing(false);
                });
        binding.fab.setOnClickListener(v -> showCreateNewProjectDialog());
        binding.toolbar.inflateMenu(R.menu.projects_menu);
        binding.toolbar.setOnMenuItemClickListener(
                item -> {
                    final var id = item.getItemId();
                    if (id == R.id.action_settings) {
                        startActivity(new Intent(this, SettingActivity.class));
                    } else if (id == R.id.action_logcat) {
                        startActivity(LynxActivity.getIntent(this, new LynxConfig()));
                    }
                    return true;
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProjects();
    }

    private void buildCreateNewProjectDialog() {
        var builder = new MaterialAlertDialogBuilder(this, AndroidUtilities.getDialogFullWidthButtonsThemeOverlay())
                .setTitle("New project")
                .setView(R.layout.create_new_project_dialog)
                .setPositiveButton("Create", null)
                .setNegativeButton("Cancel", null);
        createNewProjectDialog = builder.create();
    }

    @WorkerThread
    private void showCreateNewProjectDialog() {
        if (!createNewProjectDialog.isShowing()) {
            createNewProjectDialog.show();
            EditText input = createNewProjectDialog.findViewById(android.R.id.text1);
            Button createBtn = createNewProjectDialog.findViewById(android.R.id.button1);
            MaterialSwitch kotlinTemplate =
                    createNewProjectDialog.findViewById(R.id.use_kotlin_template);
            createBtn.setOnClickListener(
                    v -> {
                        var projectName = input.getText().toString().trim().replace("..", "");
                        if (projectName.isEmpty()) {
                            return;
                        }
                        boolean useKotlinTemplate = kotlinTemplate.isChecked();
                        try {
                            var project =
                                    useKotlinTemplate
                                            ? KotlinProject.newProject(projectName)
                                            : JavaProject.newProject(projectName);
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
                        loadProjects();
                    });
            input.setText("");
        }
    }

    @WorkerThread
    private void showDeleteProjectDialog(Project project) {
        AndroidUtilities.showSimpleAlert(this, "Delete project", getString(R.string.delete_project, project.getProjectName()), "Delete", "Cancel", ((dialog, which) -> {
            if (which == DialogInterface.BUTTON_POSITIVE) {
                runOnUiThread(
                        () -> {
                            project.delete();
                            loadProjects();
                        });
            }
        }));
    }

    @Override
    public void onProjectClicked(Project project) {
        var projectPath = project.getProjectDirPath();
        var intent = new Intent(this, MainActivity.class);
        intent.putExtra(Constants.PROJECT_PATH, projectPath);
        startActivity(intent);
    }

    @Override
    public boolean onProjectLongClicked(Project project) {
        showDeleteProjectDialog(project);
        return true;
    }

    @WorkerThread
    private void loadProjects() {
        CoroutineUtil.inParallel(
                () -> {
                    var projectDir = new File(JavaProject.getRootDirPath());
                    var directories = projectDir.listFiles(File::isDirectory);
                    var projects = new ArrayList<Project>();
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

    private void toggleNullProject(List<Project> projects) {
        if (projects.size() == 0) {
            binding.projectRecycler.setVisibility(View.GONE);
            binding.emptyContainer.setVisibility(View.VISIBLE);
        } else {
            binding.projectRecycler.setVisibility(View.VISIBLE);
            binding.emptyContainer.setVisibility(View.GONE);
        }
    }
}
