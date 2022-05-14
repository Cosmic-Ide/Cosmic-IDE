package com.pranav.java.ide.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textview.MaterialTextView;
import com.pranav.common.util.FileUtil;
import com.pranav.java.ide.MainActivity;
import com.pranav.java.ide.R;
import com.pranav.java.ide.ui.treeview.TreeNode;
import com.pranav.java.ide.ui.treeview.TreeView;
import com.pranav.java.ide.ui.treeview.binder.TreeFileNodeViewBinder;
import com.pranav.java.ide.ui.treeview.binder.TreeFileNodeViewFactory;
import com.pranav.java.ide.ui.treeview.file.TreeFile;
import com.pranav.java.ide.ui.treeview.helper.TreeCreateNewFileContent;
import com.pranav.java.ide.ui.treeview.model.TreeFolder;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TreeViewDrawer extends Fragment {

    private View mRootView;
    private TreeView<TreeFile> treeView;
    private AlertDialog createNewFileDialog, createNewDirectoryDialog, confirmDeleteDialog;

    private MainActivity activity;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.drawer_treeview, container, false);
        return mRootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        activity = ((MainActivity) getContext());

        buildCreateFileDialog();
        buildCreateDirectoryDialog();
        buildConfirmDeleteDialog();

        /* If im right:
         * List<TreeNode<TreeFile>> it's just
         * a list where we can add new ROOT nodes
         * The First Node is 'java' folder bcs it's a root dir obviously
         * So If You want to add creating a folder next to java folder function
         * You need also create a new 'new TreeNode<>(new TreeFolder(File), Int);'
         * assign a child's to it and add to rootNodesList */
        var rootNodesList =
                new ArrayList<TreeNode<TreeFile>>(); /* Create List of root nodes and and their children's */

        final var mainFolderFile =
                new File(FileUtil.getJavaDir()); /* Create File variable to Main Root Directory */
        var mainRootNode =
                new TreeNode<TreeFile>(
                        new TreeFolder(mainFolderFile),
                        0); /* Create new Root node for given Main Root Directory */

        /* Add all children directories and files to the list */
        addChildDirsAndFiles(mainRootNode, 0);
        /* Add 'java' root folder node to the list */
        rootNodesList.add(mainRootNode);

        /* Initialize TreeView */
        treeView = new TreeView<TreeFile>(requireContext(), TreeNode.root(rootNodesList));

        /* Add TreeView into HorizontalScrollView */
        HorizontalScrollView horizontalScrollView = view.findViewById(R.id.horizontalScrollView);
        horizontalScrollView.addView(
                treeView.getView(),
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        /* Finally - set adapter for TreeView and assign a listener to it */
        treeView.setAdapter(
                new TreeFileNodeViewFactory(
                        new TreeFileNodeViewBinder.TreeFileNodeListener() {
                            @Override
                            public void onNodeClicked(
                                    @Nullable View view, @Nullable TreeNode<TreeFile> treeNode) {
                                if (treeNode.getContent().getFile().isFile()
                                        && treeNode.getContent()
                                                .getFile()
                                                .getName()
                                                .endsWith(".java")) {
                                    try {
                                        activity.loadFileToEditor(
                                                treeNode.getContent().getFile().getPath());
                                        if (activity.drawer.isDrawerOpen(GravityCompat.START)) {
                                            activity.drawer.close();
                                        }
                                    } catch (Exception e) {
                                        activity.dialog("Cannot read file", e.getMessage(), true);
                                    }
                                }
                            }

                            @Override
                            public void onNodeToggled(
                                    @Nullable TreeNode<TreeFile> treeNode, boolean expanded) {}

                            @Override
                            public boolean onNodeLongClicked(
                                    @Nullable View view,
                                    @Nullable TreeNode<TreeFile> treeNode,
                                    boolean expanded) {
                                /* If long clicked item is not root : Ask user what he wanna do */
                                showPopup(view, treeNode);
                                return false;
                            }
                        }));
    }

    void addChildDirsAndFiles(TreeNode<TreeFile> mainRootNode, int n) {
        /*
         * Level 0: Root Folder
         * Level 1: Root Children's
         * Level 2: Children's Children's */
        var rootFile = ((TreeFile) mainRootNode.getValue()).getFile();
        var mFiles = getSortedFilesInPath(rootFile.getPath());
        for (final var file : mFiles) {
            if (file.isFile()) {
                /* If it's File - create file children node */
                var javaFileNode = new TreeNode<TreeFile>(new TreeFile(file), n);
                mainRootNode.addChild(javaFileNode);
            } else {
                var directoryFileNode = new TreeNode<TreeFile>(new TreeFolder(file), n);
                mainRootNode.addChild(directoryFileNode);
                n++;
                addChildDirsAndFiles(directoryFileNode, n);
            }
        }
    }

    void showPopup(View v, TreeNode<TreeFile> node) {
        var popup = new PopupMenu(activity, v);
        var inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.treeview_menu, popup.getMenu());
        popup.show();

        if (node.getLevel() == 0) {
            /* Disable Option to delete a root folder 'java' */
            popup.getMenu().getItem(2).setVisible(false);
        }

        if (node.getContent().getFile().isFile()) {
            /* We cannot create a new class or directory inside a file so we should disable these options */
            popup.getMenu().getItem(0).setVisible(false);
            popup.getMenu().getItem(1).setVisible(false);
        }

        popup.setOnMenuItemClickListener(
                new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        var id = item.getItemId();
                        if (id == R.id.create_class_menu_bttn) {
                            showCreateNewFileDialog(node);
                        } else if (id == R.id.create_directory_bttn) {
                            showCreateNewDirectoryDialog(node);
                        } else if (id == R.id.delete_menu_bttn) {
                            showConfirmDeleteDialog(node);
                        }
                        return false;
                    }
                });
    }

    void buildCreateFileDialog() {
        var builder = new MaterialAlertDialogBuilder(getContext());
        var viewGroup = activity.findViewById(android.R.id.content);
        var dialogView =
                getLayoutInflater()
                        .inflate(R.layout.treeview_create_new_file_dialog, viewGroup, false);
        builder.setView(dialogView);
        createNewFileDialog = builder.create();
    }

    void buildCreateDirectoryDialog() {
        var builder = new MaterialAlertDialogBuilder(getContext());
        ViewGroup viewGroup = activity.findViewById(android.R.id.content);
        var dialogView =
                getLayoutInflater()
                        .inflate(R.layout.treeview_create_new_folder_dialog, viewGroup, false);
        builder.setView(dialogView);
        createNewDirectoryDialog = builder.create();
    }

    void buildConfirmDeleteDialog() {
        var builder = new MaterialAlertDialogBuilder(getContext());
        ViewGroup viewGroup = activity.findViewById(android.R.id.content);
        var dialogView =
                getLayoutInflater()
                        .inflate(R.layout.treeview_confirm_delete_dialog, viewGroup, false);
        builder.setView(dialogView);
        confirmDeleteDialog = builder.create();
    }

    void showCreateNewFileDialog(TreeNode<TreeFile> node) {
        if (!createNewFileDialog.isShowing()) {
            createNewFileDialog.show();

            EditText fileName = createNewFileDialog.findViewById(R.id.fileName_edt);
            MaterialButton createBttn = createNewFileDialog.findViewById(R.id.create_bttn);

            createBttn.setOnClickListener(
                    v -> {
                        var fileNameString = fileName.getText().toString();

                        if (!fileNameString.equals("")
                                && fileNameString.length() <= 25
                                && !fileNameString.endsWith(".java")
                                && !isStringContainsNumbers(fileNameString)) {
                            try {
                                var filePth =
                                        new File(
                                                node.getContent().getFile().getPath()
                                                        + "/"
                                                        + fileNameString
                                                        + ".java");

                                if (node.getParent().getContent() == null) {
                                    FileUtil.writeFile(
                                            filePth.getAbsolutePath(),
                                            TreeCreateNewFileContent.BUILD_NEW_FILE_CONTENT(
                                                    fileNameString));
                                } else {
                                    /* Extend package name to subdirectory | example: com.example.SUBDIRECTORY; */
                                    FileUtil.writeFile(
                                            filePth.getAbsolutePath(),
                                            TreeCreateNewFileContent
                                                    .BUILD_NEW_FILE_CONTENT_EXTEND_PACKAGE(
                                                            fileNameString,
                                                            "."
                                                                    + node.getContent()
                                                                            .getFile()
                                                                            .getName()));
                                }

                                var newDir =
                                        new TreeNode<TreeFile>(
                                                new TreeFile(filePth),
                                                node.getLevel()
                                                        + 1); // Get Level of parent so it will have
                                // correct margin and disable some
                                // popup functions if needed
                                node.addChild(newDir);
                                treeView.refreshTreeView();
                                fileName.setText("");
                                createNewFileDialog.dismiss();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    void showCreateNewDirectoryDialog(TreeNode<TreeFile> node) {
        if (!createNewDirectoryDialog.isShowing()) {
            createNewDirectoryDialog.show();

            EditText fileName = createNewDirectoryDialog.findViewById(R.id.directoryName_edt);
            MaterialButton createBttn = createNewDirectoryDialog.findViewById(R.id.create_bttn);

            createBttn.setOnClickListener(
                    v -> {
                        var fileNameString = fileName.getText().toString().replace(" ", "");

                        if (fileNameString != null
                                && !fileNameString.equals("")
                                && fileNameString.length() <= 25) {
                            var filePath =
                                    node.getContent().getFile().getPath() + "/" + fileNameString;

                            FileUtil.createDirectory(filePath);
                            var dirPth = new File(filePath);
                            var newDir =
                                    new TreeNode<TreeFile>(new TreeFolder(dirPth), node.getLevel() + 1);
                            node.addChild(newDir);
                            treeView.refreshTreeView();
                            fileName.setText("");
                            createNewDirectoryDialog.dismiss();
                        } else {
                            if (fileNameString.length() <= 25) {
                                fileName.setError("Name is too long!");
                            }
                            if (fileNameString.contains("/") || fileNameString.contains(".")) {
                                fileName.setError("Illegal Char!");
                            }
                            if (isStringContainsNumbers(fileNameString)) {
                                fileName.setError("Name cannot contains digits!");
                            }
                        }
                    });
        }
    }

    void showConfirmDeleteDialog(TreeNode<TreeFile> node) {
        if (!confirmDeleteDialog.isShowing()) {
            confirmDeleteDialog.show();

            MaterialTextView areUsure_txt = confirmDeleteDialog.findViewById(R.id.areUSure_txt);
            MaterialButton confirmBttn = confirmDeleteDialog.findViewById(R.id.confirm_delete_bttn);
            MaterialButton cancelBttn = confirmDeleteDialog.findViewById(R.id.cancel_delete_button);

            areUsure_txt.setText(getString(R.string.delete, node.getContent().getFile().getName()));

            confirmBttn.setOnClickListener(
                    v -> {
                        FileUtil.deleteFile(node.getContent().getFile().getPath());
                        node.getParent().removeChild(node);
                        treeView.refreshTreeView();
                        confirmDeleteDialog.dismiss();
                    });

            cancelBttn.setOnClickListener(v -> confirmDeleteDialog.dismiss());
        }
    }

    public File file(final String path) {
        return new File(path);
    }

    public List<File> getSortedFilesInPath(String path) {
        var mFiles = new ArrayList<File>();
        var mDirs = new ArrayList<File>();

        var file = new File(path);
        var files = file.listFiles();
        if (files != null) {
            for (var child : files) {
                if (child.isFile()) {
                    mFiles.add(child);
                } else {
                    mDirs.add(child);
                }
            }
        }

        // Sort files and directories according to alphabetical order
        Collections.sort(mFiles);
        Collections.sort(mDirs);

        // Create a new arraylist which will contain the final sorted list
        var result = mDirs;
        result.addAll(mFiles);

        return result;
    }

    public boolean isStringContainsNumbers(String target) {
        var chars = target.toCharArray();
        for (var c : chars) {
            if (Character.isDigit(c)) {
                return true;
            }
        }

        return false;
    }
}
