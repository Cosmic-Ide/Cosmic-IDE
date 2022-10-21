package org.cosmic.ide.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.TextView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.ListAdapter;
import android.widget.ArrayAdapter;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
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
import org.cosmic.ide.ui.treeview.file.TreeFile;
import org.cosmic.ide.ui.treeview.model.TreeFolder;
import org.cosmic.ide.util.UiUtilsKt;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TreeViewDrawer extends Fragment {

    private TreeView<TreeFile> treeView;

    private BottomSheetDialog createNewFileDialog;
    private BottomSheetDialog createNewDirectoryDialog;
    private BottomSheetDialog confirmDeleteDialog;
    private BottomSheetDialog renameFileDialog;

    private MainActivity activity;
    private MainViewModel mainViewModel;
    private FileViewModel fileViewModel;

    public TreeViewDrawer() {
        super(R.layout.drawer_treeview);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mainViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        fileViewModel = new ViewModelProvider(requireActivity()).get(FileViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        ViewCompat.requestApplyInsets(view);
        UiUtilsKt.addSystemWindowInsetToPadding(view, false, true, false, true);

        activity = ((MainActivity) getContext());

        buildCreateFileDialog();
        buildCreateDirectoryDialog();
        buildConfirmDeleteDialog();
        buildRenameFileDialog();

        /* Initialize TreeView */
        treeView = new TreeView<TreeFile>(requireContext(), TreeNode.root(Collections.emptyList()));

        /* Add TreeView into HorizontalScrollView */
        HorizontalScrollView horizontalScrollView = view.findViewById(R.id.horizontalScrollView);
        horizontalScrollView.addView(
                treeView.getView(),
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT));

        fileViewModel
                .getNodes()
                .observe(
                        getViewLifecycleOwner(),
                        node -> {
                            treeView.refreshTreeView(node);
                        });

        SwipeRefreshLayout refreshLayout = view.findViewById(R.id.refreshLayout);
        UiUtilsKt.addSystemWindowInsetToPadding(refreshLayout, false, true, false, true);
        refreshLayout.setOnRefreshListener(
                () -> {
                    if (getActivity() != null) {
                        requireActivity()
                                .runOnUiThread(
                                        () -> {
                                            partialRefresh();
                                            refreshLayout.setRefreshing(false);
                                        });
                    }
                });

        /* Finally - set adapter for TreeView and assign a listener to it */
        treeView.setAdapter(
                new TreeFileNodeViewFactory(
                        new TreeFileNodeViewBinder.TreeFileNodeListener() {
                            @Override
                            public void onNodeToggled(
                                    @Nullable TreeNode<TreeFile> treeNode, boolean expanded) {
                                if (treeNode.isLeaf() && treeNode.getContent().getFile().isFile()) {
                                    try {
                                        mainViewModel.openFile(treeNode.getContent().getFile());
                                        if (activity.binding.root instanceof DrawerLayout) {
                                            DrawerLayout drawer =
                                                    (DrawerLayout) activity.binding.root;
                                            if (drawer != null
                                                    && drawer.isDrawerOpen(GravityCompat.START)) {
                                                mainViewModel.setDrawerState(false);
                                            }
                                        }
                                    } catch (Exception e) {
                                        activity.dialog(
                                                "Failed to open file",
                                                Log.getStackTraceString(e),
                                                true);
                                    }
                                }
                            }

                            @Override
                            public boolean onNodeLongClicked(
                                    @Nullable View view,
                                    @Nullable TreeNode<TreeFile> treeNode,
                                    boolean expanded) {
                                showPopup(view, treeNode);
                                return true;
                            }
                        }));
    }

    @Override
    public void onResume() {
        super.onResume();
        partialRefresh();
    }

    private void partialRefresh() {
        if (!treeView.getAllNodes().isEmpty()) {
            TreeNode<TreeFile> node = treeView.getAllNodes().get(0);
            TreeUtil.updateNode(node);
            treeView.refreshTreeView();
            
        }
    }

    private void showPopup(View v, TreeNode<TreeFile> node) {
        var popup = new PopupMenu(requireActivity(), v);
        var inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.treeview_menu, popup.getMenu());
        popup.show();
        final var nodeFile = node.getContent().getFile();

        if (nodeFile.getName().equals(activity.getProject().getProjectName())
                && node.getLevel() == 0) {
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
                    if (id == R.id.create_kotlin_class_menu_bttn) {
                        showCreateNewKotlinFileDialog(node);
                    } else if (id == R.id.create_java_class_menu_bttn) {
                        showCreateNewJavaFileDialog(node);
                    } else if (id == R.id.create_directory_bttn) {
                        showCreateNewDirectoryDialog(node);
                    } else if (id == R.id.delete_menu_bttn) {
                        showConfirmDeleteDialog(node);
                    } else if (id == R.id.rename_menu_bttn) {
                        showRenameFileDialog(node);
                    } else if (id == R.id.dex_menu_bttn) {
                        D8Task.compileJar(nodeFile.getAbsolutePath());
                        partialRefresh();
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
        createNewFileDialog = new BottomSheetDialog(requireContext());
        createNewFileDialog.setContentView(R.layout.treeview_create_new_file_dialog);
    }

    private void buildCreateDirectoryDialog() {
        createNewDirectoryDialog = new BottomSheetDialog(requireContext());
        createNewDirectoryDialog.setContentView(R.layout.treeview_create_new_folder_dialog);
    }

    private void buildConfirmDeleteDialog() {
        confirmDeleteDialog = new BottomSheetDialog(requireContext());
        confirmDeleteDialog.setContentView(R.layout.delete_dialog);
    }

    private void buildRenameFileDialog() {
        renameFileDialog = new BottomSheetDialog(requireContext());
        renameFileDialog.setContentView(R.layout.treeview_rename_dialog);
    }

    private void showRenameFileDialog(TreeNode<TreeFile> node) {
        if (!renameFileDialog.isShowing()) {
            renameFileDialog.show();

            EditText fileName = renameFileDialog.findViewById(android.R.id.text1);
            Button cancelBtn = renameFileDialog.findViewById(android.R.id.button2);
            Button createBtn = renameFileDialog.findViewById(android.R.id.button1);
            fileName.setText(node.getContent().getFile().getName());

            cancelBtn.setOnClickListener(v -> renameFileDialog.dismiss());
            createBtn.setOnClickListener(
                    v -> {
                        var fileNameString = fileName.getText().toString().replace("..", "");

                        if (!fileNameString.isEmpty()) {
                            try {
                                var path = Paths.get(node.getContent().getFile().getPath());
                                Files.move(path, path.resolveSibling(fileNameString));
                                partialRefresh();

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

            EditText fileName = createNewFileDialog.findViewById(android.R.id.text1);
            Button cancelBtn = createNewFileDialog.findViewById(android.R.id.button2);
            Button createBtn = createNewFileDialog.findViewById(android.R.id.button1);
            Spinner classType = createNewFileDialog.findViewById(R.id.class_kind);
            
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireActivity(), R.array.kind_class, android.R.layout.simple_spinner_item);   
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			classType.setAdapter(adapter);
			
            cancelBtn.setOnClickListener(v -> createNewFileDialog.dismiss());
            createBtn.setOnClickListener(
                    v -> {
                        var fileNameString = fileName.getText().toString().replace("..", "");
                        
                        if (!fileNameString.isEmpty()) {
                            try {
                                var filePth =
                                        new File(
                                                node.getContent().getFile().getPath()
                                                        + "/"
                                                        + fileNameString
                                                        + ".java");

                                FileUtil.writeFile(
                                        node.getContent().getFile().getPath()
                                                + "/"
                                                + fileNameString
                                                + ".java",
                                        CodeTemplate.getJavaClassTemplate(
                                                getPackageName(node.getContent().getFile()),
                                                fileNameString,
                                                false, 
                                                classType.getSelectedItem().toString()));

                                var newDir =
                                        new TreeNode<TreeFile>(
                                                new TreeFile(filePth),
                                                node.getLevel()
                                                        + 1); // Get Level of parent so it will have
                                node.addChild(newDir);
                                treeView.refreshTreeView();
                                fileName.setText("");
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

            EditText fileName = createNewFileDialog.findViewById(android.R.id.text1);
            Button cancelBtn = createNewFileDialog.findViewById(android.R.id.button2);
            Button createBtn = createNewFileDialog.findViewById(android.R.id.button1);
            Spinner classType = createNewFileDialog.findViewById(R.id.class_kind);
			
			ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireActivity(), R.array.kind_class_kotlin, android.R.layout.simple_spinner_item);   
			adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			classType.setAdapter(adapter);
			
            cancelBtn.setOnClickListener(v -> createNewFileDialog.dismiss());
            createBtn.setOnClickListener(
                    v -> {
                        var fileNameString = fileName.getText().toString().replace("..", "");

                        if (!fileNameString.isEmpty()) {
                            try {
                                var filePth =
                                        new File(
                                                node.getContent().getFile().getPath()
                                                        + "/"
                                                        + fileNameString
                                                        + ".kt");

                                FileUtil.writeFile(
                                        node.getContent().getFile().getPath()
                                                + "/"
                                                + fileNameString
                                                + ".kt",
                                        CodeTemplate.getKotlinClassTemplate(
                                                getPackageName(node.getContent().getFile()),
                                                fileNameString,
                                                false, 
                                                classType.getSelectedItem().toString()));

                                var newDir =
                                        new TreeNode<TreeFile>(
                                                new TreeFile(filePth),
                                                node.getLevel()
                                                        + 1); // Get Level of parent so it will have
                                node.addChild(newDir);
                                treeView.refreshTreeView();
                                fileName.setText("");
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

            EditText fileName = createNewDirectoryDialog.findViewById(android.R.id.text1);
            Button cancelBtn = createNewDirectoryDialog.findViewById(android.R.id.button2);
            Button createBtn = createNewDirectoryDialog.findViewById(android.R.id.button1);

            cancelBtn.setOnClickListener(v -> createNewDirectoryDialog.dismiss());
            createBtn.setOnClickListener(
                    v -> {
                        var fileNameString = fileName.getText().toString().replace("..", "");

                        if (fileNameString != null
                                && !fileNameString.isEmpty()
                                && !fileNameString.contains(".")) {
                            var filePath =
                                    node.getContent().getFile().getPath() + "/" + fileNameString;

                            FileUtil.createDirectory(filePath);
                            var dirPth = new File(filePath);
                            var newDir =
                                    new TreeNode<TreeFile>(
                                            new TreeFolder(dirPth), node.getLevel() + 1);
                            node.addChild(newDir);
                            treeView.refreshTreeView();
                            fileName.setText("");
                            createNewDirectoryDialog.dismiss();
                        } else {
                            if (fileNameString.contains(".")) {
                                fileName.setError("Illegal Char!");
                            }
                            if (fileNameString.isEmpty()) {
                                fileName.setError("Name cannot be empty");
                            }
                        }
                    });
        }
    }

    private void showConfirmDeleteDialog(TreeNode<TreeFile> node) {
        if (!confirmDeleteDialog.isShowing()) {
            confirmDeleteDialog.show();

            TextView title = confirmDeleteDialog.findViewById(android.R.id.title);
            TextView message = confirmDeleteDialog.findViewById(android.R.id.message);
            Button confirmBtn = confirmDeleteDialog.findViewById(android.R.id.button1);
            Button cancelBtn = confirmDeleteDialog.findViewById(android.R.id.button2);

            title.setText(getString(R.string.delete));
            message.setText(getString(R.string.delete_file, node.getContent().getFile().getName()));

            confirmBtn.setOnClickListener(
                    v -> {
                        FileUtil.deleteFile(node.getContent().getFile().getPath());
                        node.getParent().removeChild(node);
                        treeView.refreshTreeView();
                        confirmDeleteDialog.dismiss();
                    });

            cancelBtn.setOnClickListener(v -> confirmDeleteDialog.dismiss());
        }
    }
}
