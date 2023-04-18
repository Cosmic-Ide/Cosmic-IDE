package org.cosmicide.rewrite.treeview

import io.github.dingyi222666.view.treeview.AbstractTree
import io.github.dingyi222666.view.treeview.Tree
import io.github.dingyi222666.view.treeview.TreeNode
import io.github.dingyi222666.view.treeview.TreeNodeGenerator
import java.io.File

class FileTreeNodeGenerator(val rootItem: FileSet) : TreeNodeGenerator<FileSet> {

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
            currentData.subDir.isNotEmpty(),
            currentData.subDir.isNotEmpty(),
            false
        )
    }

    override suspend fun fetchNodeChildData(targetNode: TreeNode<FileSet>): Set<FileSet> {
        return targetNode.requireData().subDir.toSet()
    }

    override fun createRootNode(): TreeNode<FileSet> {
        return TreeNode(
            data = rootItem,
            depth = 0,
            name = rootItem.file.name,
            id = Tree.ROOT_NODE_ID,
            hasChild = true,
            isChild = true
        )
    }
}

data class FileSet(val file: File, val subDir: MutableSet<FileSet> = mutableSetOf())