/*
 * Copyright 2016 - 2017 ShineM (Xinyuan)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under.
 */
package org.cosmic.ide.ui.treeview

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Checkable
import androidx.recyclerview.widget.RecyclerView
import org.cosmic.ide.ui.treeview.base.BaseNodeViewBinder
import org.cosmic.ide.ui.treeview.base.BaseNodeViewFactory
import org.cosmic.ide.ui.treeview.base.CheckableNodeViewBinder
import org.cosmic.ide.ui.treeview.helper.TreeHelper

/** Created by xinyuanzhong on 2017/4/21.  */
class TreeViewAdapter<D> internal constructor(
    private val context: Context,
    private val root: TreeNode<D>,
    private val baseNodeViewFactory: BaseNodeViewFactory<D>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val expandedNodeList: MutableList<TreeNode<D>>?
    private var treeView: TreeView<D>? = null

    init {
        expandedNodeList = ArrayList()
        buildExpandedNodeList()
    }

    private fun buildExpandedNodeList() {
        expandedNodeList!!.clear()
        for (child in root.children) {
            insertNode(expandedNodeList, child)
        }
    }

    private fun insertNode(nodeList: MutableList<TreeNode<D>>?, treeNode: TreeNode<D>) {
        nodeList!!.add(treeNode)
        if (!treeNode.hasChild()) {
            return
        }
        if (treeNode.isExpanded) {
            for (child in treeNode.children) {
                insertNode(nodeList, child)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        // return expandedNodeList.get(position).getLevel(); // this old code row used to always
        // return the level
        val treeNode = expandedNodeList!![position]
        return baseNodeViewFactory.getViewType(treeNode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, level: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(baseNodeViewFactory.getNodeLayoutId(level), parent, false)
        val nodeViewBinder = baseNodeViewFactory.getNodeViewBinder(view, level)
        nodeViewBinder.setTreeView(treeView)
        return nodeViewBinder
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder, position: Int
    ) {
        val nodeView = holder.itemView
        val treeNode = expandedNodeList!![position]
        val viewBinder = holder as BaseNodeViewBinder<D>
        if (viewBinder.toggleTriggerViewId != 0) {
            val triggerToggleView = nodeView.findViewById<View>(viewBinder.toggleTriggerViewId)
            if (triggerToggleView != null) {
                triggerToggleView.setOnClickListener {
                    onNodeToggled(treeNode)
                    viewBinder.onNodeToggled(treeNode, treeNode.isExpanded)
                }
                triggerToggleView.setOnLongClickListener { view: View? ->
                    viewBinder.onNodeLongClicked(
                        view, treeNode, treeNode.isExpanded
                    )
                }
            }
        } else if (treeNode.isItemClickEnable) {
            nodeView.setOnClickListener {
                onNodeToggled(treeNode)
                viewBinder.onNodeToggled(treeNode, treeNode.isExpanded)
            }
            nodeView.setOnLongClickListener { view: View? ->
                viewBinder.onNodeLongClicked(
                    view,
                    treeNode,
                    treeNode.isExpanded
                )
            }
        }
        if (viewBinder is CheckableNodeViewBinder<*>) {
            setupCheckableItem(nodeView, treeNode, viewBinder as CheckableNodeViewBinder<D>)
        }
        viewBinder.bindView(treeNode)
    }

    private fun setupCheckableItem(
        nodeView: View,
        treeNode: TreeNode<D>,
        viewBinder: CheckableNodeViewBinder<D>
    ) {
        val view = nodeView.findViewById<View>(viewBinder.checkableViewId)
        if (view is Checkable) {
            val checkableView = view as Checkable
            checkableView.isChecked = treeNode.isSelected
            view.setOnClickListener {
                val checked = checkableView.isChecked
                selectNode(checked, treeNode)
                viewBinder.onNodeSelectedChanged(treeNode, checked)
            }
        } else {
            throw ClassCastException(
                "The getCheckableViewId() " + "must return a CheckBox's id"
            )
        }
    }

    fun selectNode(checked: Boolean, treeNode: TreeNode<D>) {
        treeNode.isSelected = checked
        selectChildren(treeNode, checked)
        selectParentIfNeed(treeNode, checked)
    }

    private fun selectChildren(treeNode: TreeNode<D>, checked: Boolean) {
        val impactedChildren = TreeHelper.selectNodeAndChild(treeNode, checked)
        val index = expandedNodeList!!.indexOf(treeNode)
        if (index != -1 && impactedChildren.size > 0) {
            notifyItemRangeChanged(index, impactedChildren.size + 1)
        }
    }

    private fun selectParentIfNeed(treeNode: TreeNode<D>, checked: Boolean) {
        val impactedParents = TreeHelper.selectParentIfNeedWhenNodeSelected(treeNode, checked)
        if (impactedParents.size > 0) {
            for (parent in impactedParents) {
                val position = expandedNodeList!!.indexOf(parent)
                if (position != -1) notifyItemChanged(position)
            }
        }
    }

    private fun onNodeToggled(treeNode: TreeNode<D>) {
        treeNode.isExpanded = !treeNode.isExpanded
        if (treeNode.isExpanded) {
            expandNode(treeNode)

            // expand folders recursively
            if (!treeNode.isLeaf && treeNode.children.size == 1) {
                val subNode = treeNode.children[0]
                if (!subNode.isLeaf && !subNode.isExpanded) {
                    onNodeToggled(subNode)
                }
            }
        } else {
            collapseNode(treeNode)
        }
    }

    override fun getItemCount(): Int {
        return expandedNodeList?.size ?: 0
    }

    /**
     * Refresh all,this operation is only used for refreshing list when a large of nodes have
     * changed value or structure because it take much calculation.
     */
    @SuppressLint("NotifyDataSetChanged")
    fun refreshView() {
        buildExpandedNodeList()
        notifyDataSetChanged()
    }

    // Insert a node list after index.
    private fun insertNodesAtIndex(index: Int, additionNodes: List<TreeNode<D>>?) {
        if (index < 0 || index > expandedNodeList!!.size - 1 || additionNodes == null) {
            return
        }
        expandedNodeList.addAll(index + 1, additionNodes)
        notifyItemRangeInserted(index + 1, additionNodes.size)
    }

    // Remove a node list after index.
    private fun removeNodesAtIndex(index: Int, removedNodes: List<TreeNode<D>>?) {
        if (index < 0 || index > expandedNodeList!!.size - 1 || removedNodes == null) {
            return
        }
        expandedNodeList.removeAll(removedNodes)
        notifyItemRangeRemoved(index + 1, removedNodes.size)
    }

    /** Expand node. This operation will keep the structure of children(not expand children)  */
    fun expandNode(treeNode: TreeNode<D>?) {
        if (treeNode == null) {
            return
        }
        val additionNodes = TreeHelper.expandNode(treeNode, false)
        val index = expandedNodeList!!.indexOf(treeNode)
        insertNodesAtIndex(index, additionNodes)
    }

    /** Collapse node. This operation will keep the structure of children(not collapse children)  */
    fun collapseNode(treeNode: TreeNode<D>?) {
        if (treeNode == null) {
            return
        }
        val removedNodes = TreeHelper.collapseNode(treeNode, false)
        val index = expandedNodeList!!.indexOf(treeNode)
        removeNodesAtIndex(index, removedNodes)
    }

    /** Delete a node from list.This operation will also delete its children.  */
    fun deleteNode(node: TreeNode<D>?) {
        if (node == null || node.parent == null) {
            return
        }
        val allNodes = TreeHelper.getAllNodes(
            root
        )
        if (allNodes.contains(node)) {
            node.parent.removeChild(node)
        }

        // remove children form list before delete
        collapseNode(node)
        val index = expandedNodeList!!.indexOf(node)
        if (index != -1) {
            expandedNodeList.remove(node)
        }
        notifyItemRemoved(index)
    }

    fun setTreeView(treeView: TreeView<D>?) {
        this.treeView = treeView
    }
}