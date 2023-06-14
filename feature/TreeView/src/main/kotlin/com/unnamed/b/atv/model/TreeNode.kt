/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package com.unnamed.b.atv.model

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.unnamed.b.atv.R
import com.unnamed.b.atv.view.AndroidTreeView
import com.unnamed.b.atv.view.TreeNodeWrapperView
import java.io.File
import java.util.Collections

/** Created by Bogdan Melnychuk on 2/10/15.  */
class TreeNode(var value: File?) {
    private val children = Collections.synchronizedList(ArrayList<TreeNode>())
    var id = 0
        private set
    private var mLastId = 0
    var parent: TreeNode? = null
        private set
    private var mSelected = false
    var isSelectable = true
    var viewHolder: BaseNodeViewHolder<*>? = null
        private set
    var clickListener: TreeNodeClickListener? = null
        private set
    var longClickListener: TreeNodeLongClickListener? = null
        private set
    var isExpanded = false
        private set

    fun addChildren(vararg nodes: TreeNode): TreeNode {
        for (n in nodes) {
            addChild(n)
        }
        return this
    }

    fun addChild(childNode: TreeNode): TreeNode {
        childNode.parent = this
        childNode.id = generateId()
        children.add(childNode)
        Collections.sort(children, SortFileName())
        Collections.sort(children, SortFolder())
        return this
    }

    private fun generateId(): Int {
        return ++mLastId
    }

    fun addChildren(nodes: Collection<TreeNode>): TreeNode {
        for (n in nodes) {
            addChild(n)
        }
        return this
    }

    fun childAt(index: Int): TreeNode? {
        return children.get(index)
    }

    fun deleteAllChildren() {
        children.clear()
    }

    fun deleteChild(child: TreeNode): Int {
        for (i in children.indices) {
            if (child.id == children[i].id) {
                children.removeAt(i)
                return i
            }
        }
        return -1
    }

    fun getChildren(): List<TreeNode> {
        return children
            ?: Collections.synchronizedList(ArrayList())
    }

    val isLeaf: Boolean
        get() = size() == 0

    fun size(): Int {
        return children.size
    }

    fun setValue(file: File?): TreeNode {
        value = file
        return this
    }

    fun setExpanded(expanded: Boolean): TreeNode {
        isExpanded = expanded
        return this
    }

    var isSelected: Boolean
        get() = isSelectable && mSelected
        set(selected) {
            mSelected = selected
        }
    val path: String
        get() {
            val path = StringBuilder()
            var node: TreeNode? = this
            while (node!!.parent != null) {
                path.append(node.id)
                node = node.parent
                if (node!!.parent != null) {
                    path.append(NODES_ID_SEPARATOR)
                }
            }
            return path.toString()
        }
    val level: Int
        get() {
            var level = 0
            var root: TreeNode? = this
            while (root!!.parent != null) {
                root = root.parent
                level++
            }
            return level
        }
    val isLastChild: Boolean
        get() {
            if (!isRoot) {
                val parentSize = parent!!.children.size
                if (parentSize > 0) {
                    val parentChildren: List<TreeNode>? = parent!!.children
                    return parentChildren!![parentSize - 1].id == id
                }
            }
            return false
        }
    val isRoot: Boolean
        get() = parent == null

    fun setClickListener(listener: TreeNodeClickListener?): TreeNode {
        clickListener = listener
        return this
    }

    fun setLongClickListener(listener: TreeNodeLongClickListener?): TreeNode {
        longClickListener = listener
        return this
    }

    fun setViewHolder(viewHolder: BaseNodeViewHolder<*>?): TreeNode {
        this.viewHolder = viewHolder
        if (viewHolder != null) {
            viewHolder.mNode = this
        }
        return this
    }

    val isFirstChild: Boolean
        get() {
            if (!isRoot) {
                val parentChildren: List<TreeNode>? = parent!!.children
                return parentChildren!![0].id == id
            }
            return false
        }

    fun getRoot(): TreeNode {
        var root: TreeNode? = this
        while (root!!.parent != null) {
            root = root.parent
        }
        return root
    }

    abstract class BaseNodeViewHolder<E>(protected var context: Context) {
        protected var tView: AndroidTreeView? = null
        var mNode: TreeNode? = null
        var containerStyle = 0
        private var mView: View? = null
        fun setTreeViev(treeViev: AndroidTreeView?) {
            tView = treeViev
        }

        val treeView: AndroidTreeView?
            get() = tView
        open val nodeItemsView: ViewGroup
            get() = view!!.findViewById<View>(R.id.node_items) as ViewGroup
        val view: View?
            get() {
                if (mView != null) {
                    return mView
                }
                val nodeView = nodeView
                val nodeWrapperView = TreeNodeWrapperView(
                    nodeView?.context,
                    containerStyle
                )
                nodeWrapperView.insertNodeView(nodeView)
                mView = nodeWrapperView
                return mView
            }
        val nodeView: View?
            get() = createNodeView(mNode, mNode!!.value as E?)

        abstract fun createNodeView(node: TreeNode?, value: E?): View?
        val isInitialized: Boolean
            get() = mView != null

        open fun toggle(active: Boolean) {
            // empty
        }

        fun toggleSelectionMode() {
            // empty
        }
    }

    inner class SortFileName : Comparator<TreeNode> {
        override fun compare(f1: TreeNode, f2: TreeNode): Int {
            return f1.value!!.name.compareTo(f2.value!!.name)
        }
    }

    inner class SortFolder : Comparator<TreeNode> {
        override fun compare(p1: TreeNode, p2: TreeNode): Int {
            val f1 = p1.value
            val f2 = p2.value
            return if (f1!!.isDirectory == f2!!.isDirectory) 0 else if (f1.isDirectory && !f2.isDirectory) -1 else 1
        }
    }

    interface TreeNodeClickListener {
        fun onClick(node: TreeNode?, value: Any?)
    }

    interface TreeNodeLongClickListener {
        fun onLongClick(node: TreeNode?, value: Any?): Boolean
    }

    companion object {
        const val NODES_ID_SEPARATOR = ":"

        @JvmOverloads
        fun root(value: File? = null): TreeNode {
            val root = TreeNode(value)
            root.isSelectable = false
            return root
        }
    }
}
