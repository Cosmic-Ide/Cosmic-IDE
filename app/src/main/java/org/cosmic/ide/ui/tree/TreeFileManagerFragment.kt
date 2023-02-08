package org.cosmic.ide.ui.tree

import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.Spinner
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import org.cosmic.ide.R
import org.cosmic.ide.activity.MainActivity
import org.cosmic.ide.activity.model.FileViewModel
import org.cosmic.ide.activity.model.MainViewModel
import org.cosmic.ide.android.task.dex.D8Task.Companion.compileJar
import org.cosmic.ide.common.util.FileUtil.createDirectory
import org.cosmic.ide.common.util.FileUtil.deleteFile
import org.cosmic.ide.common.util.FileUtil.writeFile
import org.cosmic.ide.project.CodeTemplate
import org.cosmic.ide.ui.treeview.TreeNode
import org.cosmic.ide.ui.treeview.TreeUtil
import org.cosmic.ide.ui.treeview.TreeView
import org.cosmic.ide.ui.treeview.binder.TreeFileNodeViewBinder.TreeFileNodeListener
import org.cosmic.ide.ui.treeview.binder.TreeFileNodeViewFactory
import org.cosmic.ide.ui.treeview.model.TreeFile
import org.cosmic.ide.ui.treeview.model.TreeFolder
import org.cosmic.ide.util.AndroidUtilities.copyToClipboard
import org.cosmic.ide.util.AndroidUtilities.dialogFullWidthButtonsThemeOverlay
import org.cosmic.ide.util.AndroidUtilities.showSimpleAlert
import org.cosmic.ide.util.addSystemWindowInsetToPadding
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

class TreeFileManagerFragment : Fragment(R.layout.tree_file_manager_fragment) {
    private var treeView: TreeView<TreeFile>? = null
    private var createNewFileDialog: AlertDialog? = null
    private var createNewDirectoryDialog: AlertDialog? = null
    private var renameFileDialog: AlertDialog? = null
    private val activity: MainActivity
        get() = requireActivity() as MainActivity
    private var mainViewModel: MainViewModel? = null
    private var fileViewModel: FileViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainViewModel = ViewModelProvider(activity)[MainViewModel::class.java]
        fileViewModel = ViewModelProvider(activity)[FileViewModel::class.java]
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        ViewCompat.requestApplyInsets(view)
        view.addSystemWindowInsetToPadding(top = true, bottom = true)
        val refreshLayout = view.findViewById<SwipeRefreshLayout>(R.id.refreshLayout)
        refreshLayout.setOnRefreshListener {
            partialRefresh {
                refreshLayout.isRefreshing = false
                treeView!!.refreshTreeView()
            }
        }
        buildCreateFileDialog()
        buildCreateDirectoryDialog()
        buildRenameFileDialog()
        treeView = TreeView(activity, TreeNode.root(emptyList()))
        val horizontalScrollView =
            view.findViewById<HorizontalScrollView>(R.id.horizontalScrollView)
        horizontalScrollView.addView(
            treeView!!.view,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        treeView!!.view.isNestedScrollingEnabled = false
        treeView!!.setAdapter(
            TreeFileNodeViewFactory(
                object : TreeFileNodeListener {
                    override fun onNodeToggled(
                        treeNode: TreeNode<TreeFile>?, expanded: Boolean
                    ) {
                        if (treeNode!!.isLeaf) {
                            try {
                                val file = treeNode.value.getFile()
                                if (file.isFile) {
                                    mainViewModel!!.openFile(file)
                                    mainViewModel!!.setDrawerState(false)
                                }
                            } catch (e: Exception) {
                                showSimpleAlert(
                                    activity,
                                    activity.getString(R.string.error_file_open),
                                    e.localizedMessage,
                                    activity.getString(R.string.dialog_close),
                                    activity.getString(R.string.copy_stacktrace),
                                    null
                                ) { _, which ->
                                    if (which == DialogInterface.BUTTON_NEGATIVE) {
                                        copyToClipboard(
                                            e.localizedMessage
                                        )
                                    }
                                }
                            }
                        }
                    }

                    override fun onNodeLongClicked(
                        view: View?, treeNode: TreeNode<TreeFile>?, expanded: Boolean
                    ): Boolean {
                        showPopup(view, treeNode)
                        return true
                    }
                })
        )
        fileViewModel!!
            .nodes
            .observe(
                viewLifecycleOwner
            ) { node: TreeNode<TreeFile>? ->
                treeView!!.refreshTreeView(
                    node!!
                )
            }
    }

    private fun partialRefresh(callback: Runnable) {
        if (treeView!!.allNodes.isNotEmpty()) {
            val node = treeView!!.allNodes[0]
            TreeUtil.updateNode(node)
            if (getActivity() != null) {
                callback.run()
            }
        }
    }

    private fun showPopup(v: View?, treeNode: TreeNode<TreeFile>?) {
        val popup = PopupMenu(activity, v!!)
        val inflater = popup.menuInflater
        inflater.inflate(R.menu.treeview_menu, popup.menu)
        popup.show()
        val nodeFile = treeNode!!.value.getFile()
        if (nodeFile.isFile) {
            popup.menu.getItem(0).isVisible = false
            popup.menu.getItem(1).isVisible = false
            popup.menu.getItem(2).isVisible = false
        }
        if (nodeFile.extension == "jar") {
            popup.menu.getItem(5).isVisible = true
        }
        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.create_kotlin_class_menu_btn ->
                    showCreateNewKotlinFileDialog(treeNode)

                R.id.create_java_class_menu_btn ->
                    showCreateNewJavaFileDialog(treeNode)

                R.id.create_directory_btn ->
                    showCreateNewDirectoryDialog(treeNode)

                R.id.delete_menu_btn ->
                    showConfirmDeleteDialog(treeNode)

                R.id.rename_menu_btn ->
                    showRenameFileDialog(treeNode)

                R.id.dex_menu_btn ->
                    compileJar(nodeFile.absolutePath)
            }
            partialRefresh { treeView!!.refreshTreeView() }
            true
        }
    }

    private fun getPackageName(file: File): String {
        val pkgMatcher = Pattern.compile("src").matcher(file.absolutePath)
        if (pkgMatcher.find()) {
            val end = pkgMatcher.end()
            if (end <= 0) return ""
            var name = file.absolutePath.substring(pkgMatcher.end())
            if (name.startsWith(File.separator)) {
                name = name.substring(1)
            }
            return name.replace(File.separator, ".")
        }
        return ""
    }

    private fun buildCreateFileDialog() {
        val builder = MaterialAlertDialogBuilder(
            activity, dialogFullWidthButtonsThemeOverlay
        )
            .setTitle(activity.getString(R.string.create_class_dialog_title))
            .setView(R.layout.dialog_new_class)
            .setPositiveButton(
                activity.getString(R.string.create_class_dialog_positive), null
            )
            .setNegativeButton(activity.getString(android.R.string.cancel), null)
        createNewFileDialog = builder.create()
    }

    private fun buildCreateDirectoryDialog() {
        val builder = MaterialAlertDialogBuilder(
            activity, dialogFullWidthButtonsThemeOverlay
        )
            .setTitle(activity.getString(R.string.create_folder_dialog_title))
            .setView(R.layout.dialog_new_folder)
            .setPositiveButton(
                activity.getString(R.string.create_folder_dialog_positive), null
            )
            .setNegativeButton(activity.getString(android.R.string.cancel), null)
        createNewDirectoryDialog = builder.create()
    }

    private fun buildRenameFileDialog() {
        val builder = MaterialAlertDialogBuilder(
            activity, dialogFullWidthButtonsThemeOverlay
        )
            .setTitle(activity.getString(R.string.rename))
            .setView(R.layout.dialog_rename)
            .setPositiveButton(activity.getString(R.string.rename), null)
            .setNegativeButton(activity.getString(android.R.string.cancel), null)
        renameFileDialog = builder.create()
    }

    private fun showRenameFileDialog(node: TreeNode<TreeFile>?) {
        if (!renameFileDialog!!.isShowing) {
            renameFileDialog!!.show()
            val createBtn = renameFileDialog!!.findViewById<Button>(android.R.id.button1)
            val inputEt = renameFileDialog!!.findViewById<EditText>(android.R.id.text1)
            inputEt!!.setText(node!!.value.getFile().name)
            createBtn!!.setOnClickListener {
                val fileName = inputEt.text.toString().replace("..", "")
                if (fileName.isNotEmpty()) {
                    try {
                        val path = Paths.get(node.value.getFile().path)
                        Files.move(path, path.resolveSibling(fileName))
                        partialRefresh { treeView!!.refreshTreeView() }
                        renameFileDialog!!.dismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun showCreateNewJavaFileDialog(node: TreeNode<TreeFile>?) {
        if (!createNewFileDialog!!.isShowing) {
            createNewFileDialog!!.show()
            (createNewFileDialog!!.findViewById<View>(R.id.til_input) as TextInputLayout?)
                ?.suffixText = ".java"
            val inputEt = createNewFileDialog!!.findViewById<EditText>(android.R.id.text1)
            val createBtn = createNewFileDialog!!.findViewById<Button>(android.R.id.button1)
            val classType = createNewFileDialog!!.findViewById<Spinner>(R.id.class_kind)
            val adapter = ArrayAdapter.createFromResource(
                activity, R.array.kind_class, android.R.layout.simple_spinner_item
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            classType!!.adapter = adapter
            createBtn!!.setOnClickListener {
                val fileName = inputEt!!.text.toString().replace("..", "")
                if (fileName.isNotEmpty()) {
                    try {
                        val filePth = File(
                            node!!.value.getFile().path
                                    + "/"
                                    + fileName
                                    + ".java"
                        )
                        writeFile(
                            node.value.getFile().path
                                    + "/"
                                    + fileName
                                    + ".java",
                            CodeTemplate.getJavaClassTemplate(
                                getPackageName(node.value.getFile()),
                                fileName,
                                false,
                                classType.selectedItem.toString()
                            )
                        )
                        val newDir = TreeNode(
                            TreeFile(filePth), node.level
                                    + 1
                        ) // Get Level of parent so it will have
                        node.addChild(newDir)
                        treeView!!.refreshTreeView()
                        inputEt.setText("")
                        createNewFileDialog!!.dismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun showCreateNewKotlinFileDialog(node: TreeNode<TreeFile>?) {
        if (!createNewFileDialog!!.isShowing) {
            createNewFileDialog!!.show()
            (createNewFileDialog!!.findViewById<View>(R.id.til_input) as TextInputLayout?)?.suffixText =
                ".kt"
            val inputEt = createNewFileDialog!!.findViewById<EditText>(android.R.id.text1)
            val createBtn = createNewFileDialog!!.findViewById<Button>(android.R.id.button1)
            val classType = createNewFileDialog!!.findViewById<Spinner>(R.id.class_kind)
            val adapter = ArrayAdapter.createFromResource(
                activity,
                R.array.kind_class_kotlin,
                android.R.layout.simple_spinner_item
            )
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            classType!!.adapter = adapter
            createBtn!!.setOnClickListener {
                val fileName = inputEt!!.text.toString().replace("..", "")
                if (fileName.isNotEmpty()) {
                    try {
                        val filePth = File(
                            node!!.value.getFile().path
                                    + "/"
                                    + fileName
                                    + ".kt"
                        )
                        writeFile(
                            node.value.getFile().path
                                    + "/"
                                    + fileName
                                    + ".kt",
                            CodeTemplate.getKotlinClassTemplate(
                                getPackageName(node.value.getFile()),
                                fileName,
                                false,
                                classType.selectedItem.toString()
                            )
                        )
                        val newDir = TreeNode(
                            TreeFile(filePth), node.level
                                    + 1
                        ) // Get Level of parent so it will have
                        node.addChild(newDir)
                        treeView!!.refreshTreeView()
                        inputEt.setText("")
                        createNewFileDialog!!.dismiss()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    private fun showCreateNewDirectoryDialog(node: TreeNode<TreeFile>?) {
        if (!createNewDirectoryDialog!!.isShowing) {
            createNewDirectoryDialog!!.show()
            val inputEt = createNewDirectoryDialog!!.findViewById<EditText>(android.R.id.text1)
            val createBtn = createNewDirectoryDialog!!.findViewById<Button>(android.R.id.button1)
            createBtn!!.setOnClickListener {
                val fileName = inputEt!!.text.toString().replace("..", "")
                if (fileName.isNotEmpty() && !fileName.contains(".")) {
                    val filePath = node!!.value.getFile().path + "/" + fileName
                    createDirectory(filePath)
                    val dirPth = File(filePath)
                    val newDir = TreeNode<TreeFile>(
                        TreeFolder(dirPth), node.level + 1
                    )
                    node.addChild(newDir)
                    treeView!!.refreshTreeView()
                    inputEt.setText("")
                    createNewDirectoryDialog!!.dismiss()
                } else {
                    if (fileName.contains(".") || fileName.isEmpty()) {
                        (inputEt.parent as TextInputLayout).error = activity.getString(
                            R.string.create_folder_dialog_invalid_name
                        )
                    }
                }
            }
        }
    }

    private fun showConfirmDeleteDialog(node: TreeNode<TreeFile>?) {
        showSimpleAlert(
            activity,
            activity.getString(R.string.dialog_delete),
            getString(R.string.dialog_confirm_delete, node!!.value.getFile().name),
            activity.getString(android.R.string.ok),
            activity.getString(android.R.string.cancel),
            null
        ) { _, which ->
            if (which == DialogInterface.BUTTON_POSITIVE) {
                deleteFile(node.value.getFile().path)
                node.parent.removeChild(node)
                treeView!!.refreshTreeView()
            }
        }
    }
}