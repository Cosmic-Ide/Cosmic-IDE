package org.cosmic.ide.ui.tree;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import org.cosmic.ide.R;
import org.cosmic.ide.activity.MainActivity;
import org.cosmic.ide.activity.model.FileViewModel;
import org.cosmic.ide.activity.model.MainViewModel;
import org.cosmic.ide.android.task.dex.D8Task;
import org.cosmic.ide.common.util.FileUtil;
import org.cosmic.ide.project.CodeTemplate;
import org.cosmic.ide.ui.treeview.TreeNode;
import org.cosmic.ide.ui.treeview.TreeUtil;
import org.cosmic.ide.ui.treeview.TreeView;
import org.cosmic.ide.ui.treeview.binder.TreeFileNodeViewBinder;
import org.cosmic.ide.ui.treeview.binder.TreeFileNodeViewFactory;
import org.cosmic.ide.ui.treeview.model.TreeFile;
import org.cosmic.ide.ui.treeview.model.TreeFolder;
import org.cosmic.ide.util.AndroidUtilities;
import org.cosmic.ide.util.UiUtilsKt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeFileManagerFragment extends Fragment {

    private TreeView<TreeFile> treeView;

    private AlertDialog createNewFileDialog;
    private AlertDialog createNewDirectoryDialog;
    private AlertDialog renameFileDialog;

    private MainActivity activity;
    private MainViewModel mainViewModel;
    private FileViewModel fileViewModel;

    public TreeFileManagerFragment() {
        super(R.layout.tree_file_manager_fragment);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = ((MainActivity) getContext());

        mainViewModel = new ViewModelProvider(activity).get(MainViewModel.class);
        fileViewModel = new ViewModelProvider(activity).get(FileViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewCompat.requestApplyInsets(view);
        UiUtilsKt.addSystemWindowInsetToPadding(view, false, true, false, true);

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(
                () ->
                        partialRefresh(
                                () -> {
                                    refreshLayout.setRefreshing(false);
                                    treeView.refreshTreeView();
                                }));

        buildCreateFileDialog();
        buildCreateDirectoryDialog();
        buildRenameFileDialog();

        treeView = new TreeView<TreeFile>(activity, TreeNode.root(Collections.emptyList()));

        HorizontalScrollView horizontalScrollView = view.findViewById(R.id.horizontalScrollView);
        horizontalScrollView.addView(
                treeView.getView(),
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));
        treeView.getView().setNestedScrollingEnabled(false);

        boolean isGitEnabled = new File(activity.getProject().getRootFile(), ".git").exists();
        MaterialButton gitButton = view.findViewById(R.id.gitButton);
        gitButton.setText(isGitEnabled ? "DISABLE GIT" : "ENABLE GIT");
        gitButton.setOnClickListener(
                v -> {
                    AndroidUtilities.showSimpleAlert(
                            v.getContext(), "Git", "This feature is currently not available.");
                });

        treeView.setAdapter(
                new TreeFileNodeViewFactory(
                        new TreeFileNodeViewBinder.TreeFileNodeListener() {
                            @Override
                            public void onNodeToggled(
                                    TreeNode<TreeFile> treeNode, boolean expanded) {
                                if (treeNode.isLeaf()) {
                                    try {
                                        var file = treeNode.getValue().getFile();
                                        if (file.isFile()) {
                                            mainViewModel.openFile(file);
                                            mainViewModel.setDrawerState(false);
                                        }
                                    } catch (Exception e) {
                                        AndroidUtilities.showSimpleAlert(
                                                activity,
                                                activity.getString(R.string.error_file_open),
                                                e.getLocalizedMessage(),
                                                activity.getString(R.string.dialog_close),
                                                activity.getString(R.string.copy_stacktrace),
                                                ((dialog, which) -> {
                                                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                                                        AndroidUtilities.copyToClipboard(
                                                                e.getLocalizedMessage());
                                                    }
                                                }));
                                    }
                                }
                            }

                            @Override
                            public boolean onNodeLongClicked(
                                    View view, TreeNode<TreeFile> treeNode, boolean expanded) {
                                showPopup(view, treeNode);
                                return true;
                            }
                        }));
        fileViewModel
                .getNodes()
                .observe(
                        getViewLifecycleOwner(),
                        node -> {
                            treeView.refreshTreeView(node);
                        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    private void partialRefresh(Runnable callback) {
        if (!treeView.getAllNodes().isEmpty()) {
            TreeNode<TreeFile> node = treeView.getAllNodes().get(0);
            TreeUtil.updateNode(node);
            if (getActivity() != null) {
                callback.run();
            }
        }
    }

    private void showPopup(View view, TreeNode<TreeFile> treeNode) {
        var popup = new PopupMenu(activity, view);
        var inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.treeview_menu, popup.getMenu());
        popup.show();
        final var nodeFile = treeNode.getValue().getFile();

        if (nodeFile.getName().equals(activity.getProject().getProjectName())
                && treeNode.getLevel() == 0) {
            /* Disable Option to delete the root folder  */
            popup.getMenu().getItem(2).setVisible(false);
        }

        if (nodeFile.isFile()) {
            popup.getMenu().getItem(0).setVisible(false);
            popup.getMenu().getItem(1).setVisible(false);
            popup.getMenu().getItem(2).setVisible(false);
        }

        if (nodeFile.getName().endsWith(".jar")) {
            popup.getMenu().getItem(5).setVisible(true);
        }

        popup.setOnMenuItemClickListener(
                item -> {
                    var id = item.getItemId();
                    if (id == R.id.create_kotlin_class_menu_btn) {
                        showCreateNewKotlinFileDialog(treeNode);
                    } else if (id == R.id.create_java_class_menu_btn) {
                        showCreateNewJavaFileDialog(treeNode);
                    } else if (id == R.id.create_directory_btn) {
                        showCreateNewDirectoryDialog(treeNode);
                    } else if (id == R.id.delete_menu_btn) {
                        showConfirmDeleteDialog(treeNode);
                    } else if (id == R.id.rename_menu_btn) {
                        showRenameFileDialog(treeNode);
                    } else if (id == R.id.dex_menu_btn) {
                        D8Task.compileJar(nodeFile.getAbsolutePath());
                        partialRefresh(() -> treeView.refreshTreeView());
                    }
                    return false;
                });
    }

    private String getPackageName(final File file) {
        Matcher pkgMatcher = Pattern.compile("src").matcher(file.getAbsolutePath());
        if (pkgMatcher.find()) {
            int end = pkgMatcher.end();
            if (end <= 0) return "";
            var name = file.getAbsolutePath().substring(pkgMatcher.end());
            if (name.startsWith(File.separator)) {
                name = name.substring(1);
            }
            return name.replace(File.separator, ".");
        }
        return "";
    }

    private void buildCreateFileDialog() {
        var builder =
                new MaterialAlertDialogBuilder(
                                activity, AndroidUtilities.getDialogFullWidthButtonsThemeOverlay())
                        .setTitle(activity.getString(R.string.create_class_dialog_title))
                        .setView(R.layout.dialog_new_class)
                        .setPositiveButton(
                                activity.getString(R.string.create_class_dialog_positive), null)
                        .setNegativeButton(activity.getString(android.R.string.cancel), null);
        createNewFileDialog = builder.create();
    }

    private void buildCreateDirectoryDialog() {
        var builder =
                new MaterialAlertDialogBuilder(
                                activity, AndroidUtilities.getDialogFullWidthButtonsThemeOverlay())
                        .setTitle(activity.getString(R.string.create_folder_dialog_title))
                        .setView(R.layout.dialog_new_folder)
                        .setPositiveButton(
                                activity.getString(R.string.create_folder_dialog_positive), null)
                        .setNegativeButton(activity.getString(android.R.string.cancel), null);
        createNewDirectoryDialog = builder.create();
    }

    private void buildRenameFileDialog() {
        var builder =
                new MaterialAlertDialogBuilder(
                                activity, AndroidUtilities.getDialogFullWidthButtonsThemeOverlay())
                        .setTitle(activity.getString(R.string.rename))
                        .setView(R.layout.dialog_rename)
                        .setPositiveButton(activity.getString(R.string.rename), null)
                        .setNegativeButton(activity.getString(android.R.string.cancel), null);
        renameFileDialog = builder.create();
    }

    private void showRenameFileDialog(TreeNode<TreeFile> node) {
        if (!renameFileDialog.isShowing()) {
            renameFileDialog.show();

            Button createBtn = renameFileDialog.findViewById(android.R.id.button1);
            EditText inputEt = renameFileDialog.findViewById(android.R.id.text1);
            inputEt.setText(node.getValue().getFile().getName());

            createBtn.setOnClickListener(
                    v -> {
                        var fileName = inputEt.getText().toString().replace("..", "");

                        if (!fileName.isEmpty()) {
                            try {
                                var path = Paths.get(node.getValue().getFile().getPath());
                                Files.move(path, path.resolveSibling(fileName));
                                partialRefresh(() -> treeView.refreshTreeView());

                                renameFileDialog.dismiss();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    private void showCreateNewJavaFileDialog(TreeNode<TreeFile> node) {
        if (!createNewFileDialog.isShowing()) {
            createNewFileDialog.show();

            ((TextInputLayout) createNewFileDialog.findViewById(R.id.til_input))
                    .setSuffixText(".java");

            EditText inputEt = createNewFileDialog.findViewById(android.R.id.text1);
            Button createBtn = createNewFileDialog.findViewById(android.R.id.button1);
            Spinner classType = createNewFileDialog.findViewById(R.id.class_kind);

            ArrayAdapter<CharSequence> adapter =
                    ArrayAdapter.createFromResource(
                            activity, R.array.kind_class, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            classType.setAdapter(adapter);

            createBtn.setOnClickListener(
                    v -> {
                        var fileName = inputEt.getText().toString().replace("..", "");

                        if (!fileName.isEmpty()) {
                            try {
                                var filePth =
                                        new File(
                                                node.getValue().getFile().getPath()
                                                        + "/"
                                                        + fileName
                                                        + ".java");

                                FileUtil.writeFile(
                                        node.getValue().getFile().getPath()
                                                + "/"
                                                + fileName
                                                + ".java",
                                        CodeTemplate.getJavaClassTemplate(
                                                getPackageName(node.getValue().getFile()),
                                                fileName,
                                                false,
                                                classType.getSelectedItem().toString()));

                                var newDir =
                                        new TreeNode<TreeFile>(
                                                new TreeFile(filePth),
                                                node.getLevel()
                                                        + 1); // Get Level of parent so it will have
                                node.addChild(newDir);
                                treeView.refreshTreeView();
                                inputEt.setText("");
                                createNewFileDialog.dismiss();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    private void showCreateNewKotlinFileDialog(TreeNode<TreeFile> node) {
        if (!createNewFileDialog.isShowing()) {
            createNewFileDialog.show();

            ((TextInputLayout) createNewFileDialog.findViewById(R.id.til_input))
                    .setSuffixText(".kt");

            EditText inputEt = createNewFileDialog.findViewById(android.R.id.text1);
            Button createBtn = createNewFileDialog.findViewById(android.R.id.button1);
            Spinner classType = createNewFileDialog.findViewById(R.id.class_kind);

            ArrayAdapter<CharSequence> adapter =
                    ArrayAdapter.createFromResource(
                            activity,
                            R.array.kind_class_kotlin,
                            android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            classType.setAdapter(adapter);

            createBtn.setOnClickListener(
                    v -> {
                        var fileName = inputEt.getText().toString().replace("..", "");

                        if (!fileName.isEmpty()) {
                            try {
                                var filePth =
                                        new File(
                                                node.getValue().getFile().getPath()
                                                        + "/"
                                                        + fileName
                                                        + ".kt");

                                FileUtil.writeFile(
                                        node.getValue().getFile().getPath()
                                                + "/"
                                                + fileName
                                                + ".kt",
                                        CodeTemplate.getKotlinClassTemplate(
                                                getPackageName(node.getValue().getFile()),
                                                fileName,
                                                false,
                                                classType.getSelectedItem().toString()));

                                var newDir =
                                        new TreeNode<TreeFile>(
                                                new TreeFile(filePth),
                                                node.getLevel()
                                                        + 1); // Get Level of parent so it will have
                                node.addChild(newDir);
                                treeView.refreshTreeView();
                                inputEt.setText("");
                                createNewFileDialog.dismiss();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });
        }
    }

    private void showCreateNewDirectoryDialog(TreeNode<TreeFile> node) {
        if (!createNewDirectoryDialog.isShowing()) {
            createNewDirectoryDialog.show();

            EditText inputEt = createNewDirectoryDialog.findViewById(android.R.id.text1);
            Button createBtn = createNewDirectoryDialog.findViewById(android.R.id.button1);

            createBtn.setOnClickListener(
                    v -> {
                        var fileName = inputEt.getText().toString().replace("..", "");

                        if (fileName != null && !fileName.isEmpty() && !fileName.contains(".")) {
                            var filePath = node.getValue().getFile().getPath() + "/" + fileName;

                            FileUtil.createDirectory(filePath);
                            var dirPth = new File(filePath);
                            var newDir =
                                    new TreeNode<TreeFile>(
                                            new TreeFolder(dirPth), node.getLevel() + 1);
                            node.addChild(newDir);
                            treeView.refreshTreeView();
                            inputEt.setText("");
                            createNewDirectoryDialog.dismiss();
                        } else {
                            if (fileName.contains(".") || fileName.isEmpty()) {
                                ((TextInputLayout) inputEt.getParent())
                                        .setError(
                                                activity.getString(
                                                        R.string
                                                                .create_folder_dialog_invalid_name));
                            }
                        }
                    });
        }
    }

    private void showConfirmDeleteDialog(TreeNode<TreeFile> node) {
        AndroidUtilities.showSimpleAlert(
                activity,
                activity.getString(R.string.dialog_delete),
                getString(R.string.dialog_confirm_delete, node.getValue().getFile().getName()),
                activity.getString(android.R.string.ok),
                activity.getString(android.R.string.cancel),
                ((dialog, which) -> {
                    if (which == DialogInterface.BUTTON_POSITIVE) {
                        FileUtil.deleteFile(node.getValue().getFile().getPath());
                        node.getParent().removeChild(node);
                        treeView.refreshTreeView();
                    }
                }));
    }
}
