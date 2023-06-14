/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package com.unnamed.b.atv.view

import android.content.Context
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.Transformation
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.annotation.DrawableRes
import com.unnamed.b.atv.R
import com.unnamed.b.atv.holder.SimpleViewHolder
import com.unnamed.b.atv.model.TreeNode
import com.unnamed.b.atv.model.TreeNode.BaseNodeViewHolder
import com.unnamed.b.atv.model.TreeNode.TreeNodeClickListener
import com.unnamed.b.atv.model.TreeNode.TreeNodeLongClickListener
import java.util.Arrays

/** Created by Bogdan Melnychuk on 2/10/15.  */
class AndroidTreeView(
    private val mContext: Context,
    protected var mRoot: TreeNode,
    @param:DrawableRes private val nodeViewBackground: Int
) {
    private var applyForRoot = false
    private var containerStyle = 0
    private var defaultViewHolder: BaseNodeViewHolder<*>
    private var nodeClickListener: TreeNodeClickListener? = null
    private var nodeLongClickListener: TreeNodeLongClickListener? = null
    private var mSelectionModeEnabled = false
    private var use2dScroll = false
    var isAutoToggleEnabled = true
        private set

    init {
        defaultViewHolder = SimpleViewHolder(mContext)
    }

    fun setRoot(mRoot: TreeNode) {
        this.mRoot = mRoot
    }

    fun setDefaultContainerStyle(style: Int) {
        setDefaultContainerStyle(style, false)
    }

    fun setDefaultContainerStyle(style: Int, applyForRoot: Boolean) {
        containerStyle = style
        this.applyForRoot = applyForRoot
    }

    fun setUse2dScroll(use2dScroll: Boolean) {
        this.use2dScroll = use2dScroll
    }

    fun is2dScrollEnabled(): Boolean {
        return use2dScroll
    }

    fun setUseAutoToggle(enableAutoToggle: Boolean) {
        isAutoToggleEnabled = enableAutoToggle
    }

    fun setDefaultViewHolder(viewHolder: BaseNodeViewHolder<*>) {
        defaultViewHolder = viewHolder
    }

    fun setDefaultNodeClickListener(listener: TreeNodeClickListener?) {
        nodeClickListener = listener
    }

    fun setDefaultNodeLongClickListener(listener: TreeNodeLongClickListener?) {
        nodeLongClickListener = listener
    }

    fun expandAll() {
        expandNode(mRoot, true)
    }

    fun collapseAll() {
        for (i in mRoot.getChildren().indices) {
            val n = mRoot.childAt(i)
            collapseNode(n, true)
        }
    }

    fun getView(style: Int): View {
        val view: ViewGroup
        view = if (style > 0) {
            val newContext = ContextThemeWrapper(mContext, style)
            if (use2dScroll) TwoDScrollView(newContext) else ScrollView(newContext)
        } else {
            if (use2dScroll) TwoDScrollView(mContext) else ScrollView(mContext)
        }
        var containerContext = mContext
        if (containerStyle != 0 && applyForRoot) {
            containerContext = ContextThemeWrapper(mContext, containerStyle)
        }
        val viewTreeItems = LinearLayout(containerContext, null, containerStyle)
        viewTreeItems.id = R.id.tree_items
        viewTreeItems.orientation = LinearLayout.VERTICAL
        view.addView(viewTreeItems)
        view.isNestedScrollingEnabled = false
        mRoot.setViewHolder(
            object : BaseNodeViewHolder<Any?>(mContext) {
                override val nodeItemsView: ViewGroup
                    get() = viewTreeItems

                override fun createNodeView(node: TreeNode?, value: Any?): View? {
                    return null
                }
            })
        expandNode(mRoot, false)
        return view
    }

    val view: View
        get() = getView(-1)

    fun expandLevel(level: Int) {
        val children = mRoot.getChildren()
        for (i in children.indices) {
            val n = children[i]
            expandLevel(n, level)
        }
    }

    val saveState: String
        get() {
            val builder = StringBuilder()
            getSaveState(mRoot, builder)
            if (builder.length > 0) {
                builder.setLength(builder.length - 1)
            }
            return builder.toString()
        }

    fun restoreState(saveState: String) {
        if (!TextUtils.isEmpty(saveState)) {
            collapseAll()
            val openNodesArray =
                saveState.split(NODES_PATH_SEPARATOR.toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
            val openNodes: Set<String> = HashSet(Arrays.asList(*openNodesArray))
            restoreNodeState(mRoot, openNodes)
        }
    }

    fun toggleNode(node: TreeNode) {
        if (node.isExpanded) {
            collapseNode(node, false)
        } else {
            expandNode(node, false)
        }
    }

    @JvmOverloads
    fun collapseNode(node: TreeNode?, includeSubnodes: Boolean = false) {
        node!!.setExpanded(false)
        val nodeViewHolder = getViewHolderForNode(node)
        nodeViewHolder.nodeItemsView.visibility = View.GONE
        nodeViewHolder.toggle(false)
        if (includeSubnodes) {
            val children = node.getChildren()
            for (i in children.indices) {
                val n = children[i]
                collapseNode(n, true)
            }
        }
    }

    @JvmOverloads
    fun expandNode(node: TreeNode, includeSubnodes: Boolean = false) {
        node.setExpanded(true)
        val parentViewHolder = getViewHolderForNode(node)
        parentViewHolder.nodeItemsView.removeAllViews()
        parentViewHolder.toggle(true)
        val children = node.getChildren()
        for (i in children.indices) {
            val n = children[i]
            addNode(parentViewHolder.nodeItemsView, n)
            if (n.isExpanded || includeSubnodes) {
                expandNode(n, includeSubnodes)
            }
        }
        parentViewHolder.nodeItemsView.visibility = View.VISIBLE
    }

    fun <E> getSelectedValues(clazz: Class<E>): List<E> {
        val result: MutableList<E> = ArrayList()
        val selected = selected
        for (n in selected) {
            val value: Any? = n.value
            if (value != null && value.javaClass == clazz) {
                result.add(value as E)
            }
        }
        return result
    }

    var isSelectionModeEnabled: Boolean
        get() = mSelectionModeEnabled
        // ------------------------------------------------------------
        set(selectionModeEnabled) {
            if (!selectionModeEnabled) {
                // TODO fix double iteration over tree
                deselectAll()
            }
            mSelectionModeEnabled = selectionModeEnabled
            val children = mRoot.getChildren()
            for (i in children.indices) {
                val node = children[i]
                toggleSelectionMode(node, selectionModeEnabled)
            }
        }
    val selected: List<TreeNode>
        get() = if (mSelectionModeEnabled) {
            getSelected(mRoot)
        } else {
            ArrayList()
        }

    fun selectAll(skipCollapsed: Boolean) {
        makeAllSelection(true, skipCollapsed)
    }

    fun deselectAll() {
        makeAllSelection(selected = false, skipCollapsed = false)
    }

    fun selectNode(node: TreeNode, selected: Boolean) {
        if (mSelectionModeEnabled) {
            node.isSelected = selected
            toogleSelectionForNode(node, true)
        }
    }

    private fun toogleSelectionForNode(node: TreeNode, makeSelectable: Boolean) {
        val holder = getViewHolderForNode(node)
        if (holder.isInitialized) {
            getViewHolderForNode(node).toggleSelectionMode()
        }
    }

    private fun getViewHolderForNode(node: TreeNode?): BaseNodeViewHolder<*> {
        var viewHolder = node!!.viewHolder
        if (viewHolder == null) {
            viewHolder = defaultViewHolder
        }
        if (viewHolder.containerStyle <= 0) {
            viewHolder.containerStyle = containerStyle
        }
        if (viewHolder.treeView == null) {
            viewHolder.setTreeViev(this)
        }
        return viewHolder
    }

    fun addNode(parent: TreeNode, nodeToAdd: TreeNode) {
        parent.addChild(nodeToAdd)
        if (parent.isExpanded) {
            val parentViewHolder = getViewHolderForNode(parent)
            addNode(parentViewHolder.nodeItemsView, nodeToAdd)
        }
    }

    fun removeNode(node: TreeNode) {
        if (node.parent != null) {
            val parent = node.parent
            val index = parent!!.deleteChild(node)
            if (parent.isExpanded && index >= 0) {
                val parentViewHolder = getViewHolderForNode(parent)
                parentViewHolder.nodeItemsView.removeViewAt(index)
            }
        }
    }

    private fun expandLevel(node: TreeNode, level: Int) {
        if (node.level <= level) {
            expandNode(node, false)
        }
        val children = node.getChildren()
        var i = 0
        val childrenSize = children.size
        while (i < childrenSize) {
            val n = children[i]
            expandLevel(n, level)
            i++
        }
    }

    private fun restoreNodeState(node: TreeNode, openNodes: Set<String>) {
        val children = node.getChildren()
        var i = 0
        val childrenSize = children.size
        while (i < childrenSize) {
            val n = children[i]
            if (openNodes.contains(n.path)) {
                expandNode(n)
                restoreNodeState(n, openNodes)
            }
            i++
        }
    }

    private fun getSaveState(root: TreeNode, sBuilder: StringBuilder) {
        val children = root.getChildren()
        var i = 0
        val childrenSize = children.size
        while (i < childrenSize) {
            val node = children[i]
            if (node.isExpanded) {
                sBuilder.append(node.path)
                sBuilder.append(NODES_PATH_SEPARATOR)
                getSaveState(node, sBuilder)
            }
            i++
        }
    }

    private fun addNode(container: ViewGroup, n: TreeNode) {
        val viewHolder = getViewHolderForNode(n)
        val nodeView = viewHolder.view
        nodeView!!.setBackgroundResource(nodeViewBackground)
        if (nodeView.parent != null && nodeView.parent is ViewGroup) {
            (nodeView.parent as ViewGroup).removeView(nodeView)
        }
        container.addView(nodeView)
        if (mSelectionModeEnabled) {
            viewHolder.toggleSelectionMode()
        }
        nodeView.setOnClickListener { v: View? ->
            if (n.clickListener != null) {
                n.clickListener!!.onClick(n, n.value)
            } else if (nodeClickListener != null) {
                nodeClickListener!!.onClick(n, n.value)
            }
            if (isAutoToggleEnabled) {
                toggleNode(n)
            }
        }
        nodeView.setOnLongClickListener { view: View? ->
            if (n.longClickListener != null) {
                return@setOnLongClickListener n.longClickListener!!.onLongClick(n, n.value)
            } else if (nodeLongClickListener != null) {
                return@setOnLongClickListener nodeLongClickListener!!.onLongClick(n, n.value)
            }
            if (isAutoToggleEnabled) {
                toggleNode(n)
            }
            false
        }
    }

    private fun toggleSelectionMode(parent: TreeNode, mSelectionModeEnabled: Boolean) {
        toogleSelectionForNode(parent, mSelectionModeEnabled)
        if (parent.isExpanded) {
            val children = parent.getChildren()
            for (i in children.indices) {
                val node = children[i]
                toggleSelectionMode(node, mSelectionModeEnabled)
            }
        }
    }

    // TODO Do we need to go through whole tree? Save references or consider collapsed nodes as not
    // selected
    private fun getSelected(parent: TreeNode): List<TreeNode> {
        val result: MutableList<TreeNode> = ArrayList()
        val children = parent.getChildren()
        for (i in children.indices) {
            val n = children[i]
            if (n.isSelected) {
                result.add(n)
            }
            result.addAll(getSelected(n))
        }
        return result
    }

    // -----------------------------------------------------------------
    // Add / Remove
    private fun makeAllSelection(selected: Boolean, skipCollapsed: Boolean) {
        if (mSelectionModeEnabled) {
            val children = mRoot.getChildren()
            for (i in children.indices) {
                val node = children[i]
                selectNode(node, selected, skipCollapsed)
            }
        }
    }

    private fun selectNode(parent: TreeNode, selected: Boolean, skipCollapsed: Boolean) {
        parent.isSelected = selected
        toogleSelectionForNode(parent, true)
        val toContinue = !skipCollapsed || parent.isExpanded
        if (toContinue) {
            val children = parent.getChildren()
            for (i in children.indices) {
                val node = children[i]
                selectNode(node, selected, skipCollapsed)
            }
        }
    }

    companion object {
        const val NODES_PATH_SEPARATOR = ";"
        private fun expand(v: View) {
            v.measure(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            val targetHeight = v.measuredHeight
            v.layoutParams.height = 0
            v.visibility = View.VISIBLE
            val a: Animation = object : Animation() {
                override fun willChangeBounds(): Boolean {
                    return true
                }

                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    v.layoutParams.height =
                        if (interpolatedTime == 1f) LinearLayout.LayoutParams.WRAP_CONTENT else (targetHeight * interpolatedTime).toInt()
                    v.requestLayout()
                }
            }

            // 1dp/ms
            a.duration =
                (targetHeight / v.context.resources.displayMetrics.density / 2).toInt().toLong()
            v.startAnimation(a)
        }

        private fun collapse(v: View) {
            val initialHeight = v.measuredHeight
            val a: Animation = object : Animation() {
                override fun willChangeBounds(): Boolean {
                    return true
                }

                override fun applyTransformation(interpolatedTime: Float, t: Transformation) {
                    if (interpolatedTime == 1f) {
                        v.visibility = View.GONE
                    } else {
                        v.layoutParams.height =
                            initialHeight - (initialHeight * interpolatedTime).toInt()
                        v.requestLayout()
                    }
                }
            }

            // 1dp/ms
            a.duration =
                (initialHeight / v.context.resources.displayMetrics.density / 2).toInt().toLong()
            v.startAnimation(a)
        }
    }
}