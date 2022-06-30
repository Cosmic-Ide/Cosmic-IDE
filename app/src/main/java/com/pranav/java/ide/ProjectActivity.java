package com.pranav.java.ide;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.annotation.WorkerThread;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.pranav.java.ide.adapter.ProjectAdapter;
import com.pranav.java.ide.ui.utils.UiUtilsKt;
import com.pranav.project.mode.JavaProject;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executors;

public final class ProjectActivity extends AppCompatActivity {

    public interface OnProjectCreatedListener {
        void onProjectCreated(JavaProject project);
    }

    private RecyclerView projectRecycler; 
    private ProjectAdapter projectAdapter;

    private LinearLayout emptyContainer;

    private AlertDialog createNewProjectDialog;

    private OnProjectCreatedListener mListener;

    public void setOnProjectCreatedListener(OnProjectCreatedListener listener) {
        mListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);

        buildCreateNewProjectDialog();

        var appBarLayout = (AppBarLayout) findViewById(R.id.appbar);
        var toolbar = (MaterialToolbar) findViewById(R.id.toolbar);
        var createProjectFab = (FloatingActionButton) findViewById(R.id.fab);
        emptyContainer = findViewById(R.id.empty_container);

        setSupportActionBar(toolbar);
        UiUtilsKt.addSystemWindowInsetToPadding(appBarLayout, false, true, false, false);
        UiUtilsKt.addSystemWindowInsetToMargin(createProjectFab, false, false, false, true);

        projectAdapter = new ProjectAdapter();
        projectRecycler = findViewById(R.id.project_recycler);
        projectRecycler.setAdapter(projectAdapter);
        projectRecycler.setLayoutManager(new LinearLayoutManager(this));
        projectAdapter.setOnProjectSelectedListener(this::openProject);
        setOnProjectCreatedListener(this::openProject);

        createProjectFab.setOnClickListener(v -> showCreateNewProjectDialog());
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

    @WorkerThread
    private void showCreateNewProjectDialog() {
        if (!createNewProjectDialog.isShowing()) {
            createNewProjectDialog.show();
            EditText input = createNewProjectDialog.findViewById(android.R.id.text1);
            Button createBtn = createNewProjectDialog.findViewById(android.R.id.button1);
            createBtn.setOnClickListener(v -> {
                var projectName = String.valueOf(input.getText());
                if(TextUtils.isEmpty(projectName)) {
                    return;
                }
                try {
                    JavaProject project = JavaProject.newProject(projectName);
                    if(mListener != null) {
                        runOnUiThread(() -> {
                            if (createNewProjectDialog.isShowing()) createNewProjectDialog.dismiss();
                            mListener.onProjectCreated(project);
                        });
                    }
                } catch (IOException e){
                    e.printStackTrace();
                }
            });
        }
    }

    private void openProject(JavaProject project) {
        var projectPath = project.getProjectDirPath();
        Intent intent = new Intent(ProjectActivity.this, MainActivity.class);
        intent.putExtra("project_path", projectPath);
        startActivity(intent);
    }

    private void loadProjects() {
        Executors.newSingleThreadExecutor().execute(() -> {
            File projectDir = new File(JavaProject.getRootDirPath());
            File[] directories = projectDir.listFiles(File::isDirectory);
            List<JavaProject> projects = new ArrayList<>();
            if(directories != null) {
                Arrays.sort(directories, Comparator.comparingLong(File::lastModified));
                for(File directory : directories) {
                     File project = new File(directory, "src");
                     if(project.exists()) {
                         JavaProject javaProject = new JavaProject(new File(directory.getAbsolutePath().replace("%20", " ")));
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
        if(projects.size() == 0) {
            projectRecycler.setVisibility(View.GONE);
            emptyContainer.setVisibility(View.VISIBLE);
        } else {
            projectRecycler.setVisibility(View.VISIBLE);
            emptyContainer.setVisibility(View.GONE);
        }
    }

}