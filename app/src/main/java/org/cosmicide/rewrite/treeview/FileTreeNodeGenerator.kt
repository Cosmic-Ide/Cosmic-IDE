package org.cosmicide.rewrite.treeview

import android.util.Log
import io.github.dingyi222666.view.treeview.AbstractTree
import io.github.dingyi222666.view.treeview.Tree
import io.github.dingyi222666.view.treeview.TreeNode
import io.github.dingyi222666.view.treeview.TreeNodeGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class FileTreeNodeGenerator(private val rootItem: FileSet) : TreeNodeGenerator<FileSet> {

    override fun createNode(
        parentNode: TreeNode<FileSet>,
        currentData: FileSet,
        tree: AbstractTree<FileSet>
    ): TreeNode<FileSet> {
        return TreeNode(
            currentData,
            parentNode.depth + 1,
            currentData.file.name,
            tree.generateId(),
            currentData.file.isDirectory,
            currentData.file.isDirectory,
            false
        )
    }

    override suspend fun fetchNodeChildData(targetNode: TreeNode<FileSet>): Set<FileSet> =
        withContext(Dispatchers.IO) {
            val set = targetNode.requireData().subDir
            set.clear()
            val files = /*: Array<File> = if (targetNode.requireData().file.isFile) {
                targetNode.requireData().file.parentFile.listFiles()
            } else {*/
                targetNode.requireData().file.listFiles()
            //}
            Log.d("Refreshing Data", targetNode.requireData().file.name)
            Log.d("FILES", "Refreshing")
            for (file in files) {
                Log.d("FILES", file.name)
                when {
                    file.isFile -> set.add(FileSet(file))
                    file.isDirectory -> {
                        val tempSet = mutableSetOf<FileSet>().apply {
                            addAll(transverseTree(file))
                        }
                        set.add(FileSet(file, subDir = tempSet))
                    }
                }
            }
            return@withContext set
        }

    override fun createRootNode(): TreeNode<FileSet> {
        return TreeNode(
            data = rootItem,
            depth = 0,
            name = rootItem.file.name,
            id = Tree.ROOT_NODE_ID,
            hasChild = true,
            isChild = false
        )
    }

    private fun transverseTree(dir: File): Set<FileSet> {
        val set = mutableSetOf<FileSet>()
        val files = dir.listFiles() ?: return set
        for (file in files) {
            when {
                file.isFile -> set.add(FileSet(file))
                file.isDirectory -> {
                    val tempSet = mutableSetOf<FileSet>().apply {
                        addAll(transverseTree(file))
                    }
                    set.add(FileSet(file, subDir = tempSet))
                }
            }
        }
        return set
    }
}

data class FileSet(val file: File, val subDir: MutableSet<FileSet> = mutableSetOf())