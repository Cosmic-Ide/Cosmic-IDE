/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.treeview

import android.content.res.Resources
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Space
import androidx.annotation.MenuRes
import androidx.core.view.updateLayoutParams
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.dingyi222666.view.treeview.TreeNode
import io.github.dingyi222666.view.treeview.TreeNodeEventListener
import io.github.dingyi222666.view.treeview.TreeView
import io.github.dingyi222666.view.treeview.TreeViewBinder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.databinding.TreeviewContextActionDialogItemBinding
import org.cosmicide.rewrite.databinding.TreeviewItemDirBinding
import org.cosmicide.rewrite.databinding.TreeviewItemFileBinding
import org.cosmicide.rewrite.model.FileViewModel
import org.jetbrains.kotlin.incremental.createDirectory
import java.io.File

class ViewBinder(
    private val lifeScope: CoroutineScope,
    private val layoutInflater: LayoutInflater,
    private val fileViewModel: FileViewModel,
    private val treeView: TreeView<FileSet>
) : TreeViewBinder<FileSet>(), TreeNodeEventListener<FileSet> {
    private lateinit var dirBinding: TreeviewItemDirBinding
    private lateinit var fileBinding: TreeviewItemFileBinding

    override fun createView(parent: ViewGroup, viewType: Int): View {
        return when (viewType) {
            ViewType.DIRECTORY.ordinal -> {
                dirBinding = TreeviewItemDirBinding.inflate(layoutInflater, parent, false)
                dirBinding.root
            }

            ViewType.FILE.ordinal -> {
                fileBinding = TreeviewItemFileBinding.inflate(layoutInflater, parent, false)
                fileBinding.root
            }

            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun getItemViewType(node: TreeNode<FileSet>): Int {
        return if (node.isChild) ViewType.DIRECTORY.ordinal else ViewType.FILE.ordinal
    }

    override fun bindView(
        holder: TreeView.ViewHolder,
        node: TreeNode<FileSet>,
        listener: TreeNodeEventListener<FileSet>
    ) {
        with(holder.itemView.findViewById<Space>(R.id.space)) {
            updateLayoutParams<ViewGroup.MarginLayoutParams> {
                width = node.depth * 22.dp
            }
        }

        when {
            node.isChild -> applyDir(node)
            else -> applyFile(node)
        }
    }

    private fun applyFile(node: TreeNode<FileSet>) {
        with(fileBinding) {
            textView.text = node.name.toString()
        }
    }

    private fun applyDir(node: TreeNode<FileSet>) {
        with(dirBinding) {
            textView.text = node.name.toString()
            imageView.animate()
                .rotation(if (node.expand) 90f else 0f)
                .setDuration(200)
                .start()
        }
    }

    override fun onLongClick(node: TreeNode<FileSet>, holder: TreeView.ViewHolder): Boolean {
        showMenu(
            holder.itemView.findViewById(R.id.textView),
            R.menu.treeview_menu,
            node.data!!.file,
            node
        )
        return false
    }

    override fun onClick(node: TreeNode<FileSet>, holder: TreeView.ViewHolder) {
        when {
            node.isChild -> applyDir(node)
            else -> fileViewModel.addFile(node.data!!.file)
        }
    }

    override fun onToggle(node: TreeNode<FileSet>, isExpand: Boolean, holder: TreeView.ViewHolder) {
        applyDir(node)
    }

    private fun showMenu(v: View, @MenuRes menuRes: Int, file: File, node: TreeNode<FileSet>) {
        val popup = PopupMenu(v.context, v)
        popup.menuInflater.inflate(menuRes, popup.menu)

        when {
            node.isChild.not() -> {
                popup.menu.removeItem(R.id.create_kotlin_class)
                popup.menu.removeItem(R.id.create_java_class)
                popup.menu.removeItem(R.id.create_folder)
            }
        }

        popup.setOnMenuItemClickListener {
            val parentNode = treeView.tree.getParentNode(node)
            when (it.itemId) {
                R.id.create_kotlin_class -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    binding.textInputLayout.suffixText = ".kt"
                    MaterialAlertDialogBuilder(v.context)
                        .setTitle("Create kotlin class")
                        .setView(binding.root)
                        .setPositiveButton("Create") { _, _ ->
                            file.absolutePath
                            val name = binding.edittext.text.toString()
                            File(file, "$name.kt").createNewFile()
                            lifeScope.launch {
                                treeView.refresh(node = parentNode)
                            }
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }

                R.id.create_java_class -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    binding.textInputLayout.suffixText = ".java"
                    MaterialAlertDialogBuilder(v.context)
                        .setTitle("Create java class")
                        .setView(binding.root)
                        .setPositiveButton("Create") { _, _ ->
                            file.absolutePath
                            var name = binding.edittext.text.toString()
                            name = name.replace("\\.", "")
                            File(file, "$name.java").apply {
                                createNewFile()
                            }
                            lifeScope.launch {
                                Log.d("ViewBinder", "Refresh treeview")
                                treeView.refresh(node = parentNode)
                            }
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }

                R.id.create_folder -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    MaterialAlertDialogBuilder(v.context)
                        .setTitle("Create folder")
                        .setView(binding.root)
                        .setPositiveButton("Create") { _, _ ->
                            file.absolutePath
                            var name = binding.edittext.text.toString()
                            name = name.replace("\\.", "")
                            file.resolve(name).createDirectory()
                            lifeScope.launch {
                                Log.d("ViewBinder", "Refresh treeview")
                                treeView.refresh(node = parentNode)
                            }
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }

                R.id.rename -> {
                    val binding = TreeviewContextActionDialogItemBinding.inflate(layoutInflater)
                    binding.edittext.setText(file.name)
                    MaterialAlertDialogBuilder(v.context)
                        .setTitle("Rename")
                        .setView(binding.root)
                        .setPositiveButton("Create") { _, _ ->
                            var name = binding.edittext.text.toString()
                            name = name.replace("\\.", "")
                            file.renameTo(file.parentFile!!.resolve(name))
                            lifeScope.launch {
                                Log.d("ViewBinder", "Refresh treeview")
                                treeView.refresh(node = parentNode)
                            }
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }

                R.id.delete -> {
                    MaterialAlertDialogBuilder(v.context)
                        .setTitle("Delete")
                        .setMessage("Are you sure you want to delete this file")
                        .setPositiveButton("Create") { _, _ ->
                            file.deleteRecursively()
                            lifeScope.launch {
                                Log.d("ViewBinder", "Refresh treeview")
                                treeView.refresh(node = parentNode)
                            }
                        }
                        .setNegativeButton("Cancel") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
            true
        }
        popup.show()
    }
}

enum class ViewType {
    DIRECTORY, FILE
}

inline val Int.dp: Int
    get() = (Resources.getSystem().displayMetrics.density * this + 0.5f).toInt()