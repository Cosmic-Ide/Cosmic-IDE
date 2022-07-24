package com.pranav.java.ide;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.WorkerThread;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.pranav.common.util.CoroutineUtil;
import com.pranav.java.ide.adapter.ProjectAdapter;
import com.pranav.java.ide.ui.utils.UiUtilsKt;
import com.pranav.project.mode.JavaProject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProjectActivity extends AppCompatActivity {

    public interface OnProjectCreatedListener {
        void onProjectCreated(JavaProject project);
    }

    private RecyclerView projectRecycler; 
    private ProjectAdapter projectAdapter;

    private LinearLayout emptyContainer;

    private AlertDialog createNewProjectDialog;
    private AlertDialog deleteProjectDialog;

    private OnProjectCreatedListener mListener;

    public void setOnProjectCreatedListener(OnProjectCreatedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        buildCreateNewProjectDialog();
        buildDeleteProjectDialog();

        View scrollingView = findViewById(R.id.scrolling_view);
        var appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        var toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        var createProjectFab = (FloatingActionButton) findViewById(R.id.fab);
        emptyContainer = findViewById(R.id.empty_container);

        setSupportActionBar(toolbar);
        UiUtilsKt.addSystemWindowInsetToPadding(scrollingView, false, false, false, true);
        UiUtilsKt.addSystemWindowInsetToPadding(toolbar, false, true, false, false);
        UiUtilsKt.addSystemWindowInsetToMargin(createProjectFab, false, false, false, true);

        projectAdapter = new ProjectAdapter();
        projectRecycler = findViewById(R.id.project_recycler);
        projectRecycler.setAdapter(projectAdapter);
        projectRecycler.setLayoutManager(new LinearLayoutManager(this));
        projectAdapter.setOnProjectSelectedListener(this::openProject);
        projectAdapter.setOnProjectLongClickedListener(this::deleteProject);
        setOnProjectCreatedListener(this::openProject);

        createProjectFab.setOnClickListener(v -> showCreateNewProjectDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.projects_activity_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(ProjectActivity.this, SettingActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume() {
        super.onResume();
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
            createBtn.setOnClickListener(v -> {
                var projectName = String.valueOf(input.getText());
                if (TextUtils.isEmpty(projectName)) {
                    return;
                }
                try {
                    var project = JavaProject.newProject(projectName);
                    if (mListener != null) {
                        runOnUiThread(() -> {
                            if (createNewProjectDialog.isShowing()) createNewProjectDialog.dismiss();
                            mListener.onProjectCreated(project);
                        });
                    }
                } catch (IOException e){
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
            message.setText("Are you sure you want to delete the " + project.getProjectName() + " project?");
            deleteBtn.setOnClickListener(v -> {
                runOnUiThread(() -> {
                    if (deleteProjectDialog.isShowing()) deleteProjectDialog.dismiss();
                    project.delete();
                    loadProjects();
                });
            });
        }
    }

    private void openProject(JavaProject project) {
        var projectPath = project.getProjectDirPath();
        var intent = new Intent(ProjectActivity.this, MainActivity.class);
        intent.putExtra("project_path", projectPath);
        startActivity(intent);
    }

    private boolean deleteProject(JavaProject project) {
        showDeleteProjectDialog(project);
        return true;
    }

    private void loadProjects() {
        CoroutineUtil.inParallel(() -> {
            var projectDir = new File(JavaProject.getRootDirPath());
            var directories = projectDir.listFiles(File::isDirectory);
            var projects = new ArrayList<JavaProject>();
            if (directories != null) {
                Arrays.sort(directories, Comparator.comparingLong(File::lastModified));
                for (var directory : directories) {
                     var project = new File(directory, "src");
                     if (project.exists()) {
                         var javaProject = new JavaProject(new File(directory.getAbsolutePath()));
                         projects.add(javaProject);
                     }
                }
            }
            runOnUiThread(() -> {
                projectAdapter.submitList(projects);
                toggleNullProject(projects);
            });
        });
    }

    private void toggleNullProject(List<JavaProject> projects) {
        if (projects.size() == 0) {
            projectRecycler.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
        } else {
            projectRecycler.setVisibility(View.VISIBLE);
            emptyContainer.setVisibility(View.GONE);
        }
    }
}