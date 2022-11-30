package org.cosmic.ide.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.github.pedrovgs.lynx.LynxActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;

import org.cosmic.ide.R;
import org.cosmic.ide.activity.adapter.ProjectAdapter;
import org.cosmic.ide.common.util.CoroutineUtil;
import org.cosmic.ide.databinding.ActivityProjectBinding;
import org.cosmic.ide.databinding.DialogNewProjectBinding;
import org.cosmic.ide.git.model.Author;
import org.cosmic.ide.git.usecases.UseCasesKt;
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

public class ProjectActivity extends BaseActivity<ActivityProjectBinding>
        implements ProjectAdapter.OnProjectEventListener {

    public interface OnProjectCreatedListener {
        void onProjectCreated(Project project);
    }

    private ProjectAdapter projectAdapter;

    private AlertDialog createNewProjectDialog;
    private DialogNewProjectBinding projectBinding;

    private OnProjectCreatedListener mListener;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(ActivityProjectBinding.inflate(getLayoutInflater()));
        setSupportActionBar(binding.toolbar);

        buildCreateNewProjectDialog();

        projectAdapter = new ProjectAdapter();
        binding.projectRecycler.setAdapter(projectAdapter);
        binding.projectRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.projectRecycler.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        projectAdapter.setOnProjectEventListener(this);
        mListener = this::onProjectClicked;

        UiUtilsKt.addSystemWindowInsetToMargin(binding.fab, false, false, false, true);
        UiUtilsKt.addSystemWindowInsetToPadding(binding.appBar, false, true, false, false);
        UiUtilsKt.addSystemWindowInsetToPadding(binding.projectRecycler, false, false, false, true);

        binding.refreshLayout.setOnRefreshListener(
                () -> {
                    loadProjects();
                    binding.refreshLayout.setRefreshing(false);
                });
        binding.fab.setOnClickListener(v -> showCreateNewProjectDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.projects_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final var id = item.getItemId();
        if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        } else if (id == R.id.action_logcat) {
            startActivity(LynxActivity.getIntent(this));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
        loadProjects();
    }

    @Override
    public int getLayoutRes() {
        return R.layout.activity_project;
    }

    private void buildCreateNewProjectDialog() {
        var builder =
                new MaterialAlertDialogBuilder(
                                this, AndroidUtilities.getDialogFullWidthButtonsThemeOverlay())
                        .setTitle(getString(R.string.create_project));
        projectBinding = DialogNewProjectBinding.inflate(LayoutInflater.from(builder.getContext()));
        builder
                .setView(projectBinding.getRoot())
                .setPositiveButton(getString(R.string.create), null)
                .setNegativeButton(getString(android.R.string.cancel), null);
        createNewProjectDialog = builder.create();
    }

    @WorkerThread
    private void showCreateNewProjectDialog() {
        if (!createNewProjectDialog.isShowing()) {
            createNewProjectDialog.show();
            Button createBtn = createNewProjectDialog.findViewById(android.R.id.button1);
            createBtn.setOnClickListener(
                    v -> {
                        var projectName = projectBinding.text1.getText().toString().trim().replace("..", "");
                        if (projectName.isEmpty()) {
                            return;
                        }
                        boolean useKotlinTemplate = projectBinding.useKotlinTemplate.isChecked();

                        try {
                            var project =
                                    useKotlinTemplate
                                            ? KotlinProject.newProject(projectName)
                                            : JavaProject.newProject(projectName);
                            if (projectBinding.useGit.isChecked()) {
                                final var author = new Author(getSettings().getGitUserName(), getSettings().getGitUserEmail());
                                UseCasesKt.createGitRepoWith(project.getProjectDirPath(), author, "Initial Commit");
                            }
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
            projectBinding.text1.setText("");
        }
    }

    @WorkerThread
    private void showDeleteProjectDialog(Project project) {
        AndroidUtilities.showSimpleAlert(
                this,
                getString(R.string.dialog_delete),
                getString(R.string.dialog_confirm_delete, project.getProjectName()),
                getString(android.R.string.yes),
                getString(android.R.string.no),
                ((dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        project.delete();
                        runOnUiThread(this::loadProjects);
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
