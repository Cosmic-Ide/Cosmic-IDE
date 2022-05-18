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

package com.pranav.java.ide.ui.treeview;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pranav.java.ide.ui.treeview.base.BaseNodeViewBinder;
import com.pranav.java.ide.ui.treeview.base.BaseNodeViewFactory;
import com.pranav.java.ide.ui.treeview.base.CheckableNodeViewBinder;
import com.pranav.java.ide.ui.treeview.helper.TreeHelper;

import java.util.ArrayList;
import java.util.List;

/** Created by xinyuanzhong on 2017/4/21. */
public class TreeViewAdapter<D> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;

    private final TreeNode<D> root;

    private final List<TreeNode<D>> expandedNodeList;

    private final BaseNodeViewFactory<D> baseNodeViewFactory;

    private TreeView<D> treeView;

    TreeViewAdapter(
            Context context,
            TreeNode<D> root,
            @NonNull BaseNodeViewFactory<D> baseNodeViewFactory) {
        this.context = context;
        this.root = root;
        this.baseNodeViewFactory = baseNodeViewFactory;

        this.expandedNodeList = new ArrayList<>();

        buildExpandedNodeList();
    }

    private void buildExpandedNodeList() {
        expandedNodeList.clear();

        for (var child : root.getChildren()) {
            insertNode(expandedNodeList, child);
        }
    }

    private void insertNode(List<TreeNode<D>> nodeList, TreeNode<D> treeNode) {
        nodeList.add(treeNode);

        if (!treeNode.hasChild()) {
            return;
        }
        if (treeNode.isExpanded()) {
            for (var child : treeNode.getChildren()) {
                insertNode(nodeList, child);
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        // return expandedNodeList.get(position).getLevel(); // this old code row used to always
        // return the level
        var treeNode = expandedNodeList.get(position);
        return this.baseNodeViewFactory.getViewType(treeNode);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int level) {
        var view =
                LayoutInflater.from(context)
                        .inflate(baseNodeViewFactory.getNodeLayoutId(level), parent, false);

        var nodeViewBinder = baseNodeViewFactory.getNodeViewBinder(view, level);
        nodeViewBinder.setTreeView(treeView);
        return nodeViewBinder;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onBindViewHolder(
            @NonNull final RecyclerView.ViewHolder holder, final int position) {
        final var nodeView = holder.itemView;
        final var treeNode = expandedNodeList.get(position);
        final var viewBinder = (BaseNodeViewBinder<D>) holder;

        if (viewBinder.getToggleTriggerViewId() != 0) {
            var triggerToggleView = nodeView.findViewById(viewBinder.getToggleTriggerViewId());

            if (triggerToggleView != null) {
                triggerToggleView.setOnClickListener(
                        v -> {
                            onNodeToggled(treeNode);
                            viewBinder.onNodeClicked(v, treeNode);
                            viewBinder.onNodeToggled(treeNode, treeNode.isExpanded());
                        });

                triggerToggleView.setOnLongClickListener(
                        view -> {
                            return viewBinder.onNodeLongClicked(
                                    view, treeNode, treeNode.isExpanded());
                        });
            }
        } else if (treeNode.isItemClickEnable()) {
            nodeView.setOnClickListener(
                    v -> {
                        onNodeToggled(treeNode);
                        viewBinder.onNodeClicked(v, treeNode);
                        viewBinder.onNodeToggled(treeNode, treeNode.isExpanded());
                    });

            nodeView.setOnLongClickListener(
                    view -> viewBinder.onNodeLongClicked(view, treeNode, treeNode.isExpanded()));
        }

        if (viewBinder instanceof CheckableNodeViewBinder) {
            setupCheckableItem(nodeView, treeNode, (CheckableNodeViewBinder<D>) viewBinder);
        }

        viewBinder.bindView(treeNode);
    }

    private void setupCheckableItem(
            View nodeView,
            final TreeNode<D> treeNode,
            final CheckableNodeViewBinder<D> viewBinder) {
        final var view = nodeView.findViewById(viewBinder.getCheckableViewId());

        if (view instanceof Checkable) {
            final var checkableView = (Checkable) view;
            checkableView.setChecked(treeNode.isSelected());

            view.setOnClickListener(
                    v -> {
                        var checked = checkableView.isChecked();
                        selectNode(checked, treeNode);
                        viewBinder.onNodeSelectedChanged(treeNode, checked);
                    });
        } else {
            throw new ClassCastException(
                    "The getCheckableViewId() " + "must return a CheckBox's id");
        }
    }

    void selectNode(boolean checked, TreeNode<D> treeNode) {
        treeNode.setSelected(checked);

        selectChildren(treeNode, checked);
        selectParentIfNeed(treeNode, checked);
    }

    private void selectChildren(TreeNode<D> treeNode, boolean checked) {
        var impactedChildren = TreeHelper.selectNodeAndChild(treeNode, checked);
        int index = expandedNodeList.indexOf(treeNode);
        if (index != -1 && impactedChildren.size() > 0) {
            notifyItemRangeChanged(index, impactedChildren.size() + 1);
        }
    }

    private void selectParentIfNeed(TreeNode<D> treeNode, boolean checked) {
        var impactedParents = TreeHelper.selectParentIfNeedWhenNodeSelected(treeNode, checked);
        if (impactedParents.size() > 0) {
            for (var parent : impactedParents) {
                var position = expandedNodeList.indexOf(parent);
                if (position != -1) notifyItemChanged(position);
            }
        }
    }

    public void onNodeToggled(TreeNode<D> treeNode) {
        treeNode.setExpanded(!treeNode.isExpanded());

        if (treeNode.isExpanded()) {
            expandNode(treeNode);

            // expand folders recursively
            if (!treeNode.isLeaf() && treeNode.getChildren().size() == 1) {
                var subNode = treeNode.getChildren().get(0);

                if (!subNode.isLeaf() && !subNode.isExpanded()) {
                    onNodeToggled(subNode);
                }
            }

        } else {
            collapseNode(treeNode);
        }
    }

    @Override
    public int getItemCount() {
        return expandedNodeList == null ? 0 : expandedNodeList.size();
    }

    /**
     * Refresh all,this operation is only used for refreshing list when a large of nodes have
     * changed value or structure because it take much calculation.
     */
    @SuppressLint("NotifyDataSetChanged")
    void refreshView() {
        buildExpandedNodeList();
        notifyDataSetChanged();
    }

    // Insert a node list after index.
    private void insertNodesAtIndex(int index, List<TreeNode<D>> additionNodes) {
        if (index < 0 || index > expandedNodeList.size() - 1 || additionNodes == null) {
            return;
        }
        expandedNodeList.addAll(index + 1, additionNodes);
        notifyItemRangeInserted(index + 1, additionNodes.size());
    }

    // Remove a node list after index.
    private void removeNodesAtIndex(int index, List<TreeNode<D>> removedNodes) {
        if (index < 0 || index > expandedNodeList.size() - 1 || removedNodes == null) {
            return;
        }
        expandedNodeList.removeAll(removedNodes);
        notifyItemRangeRemoved(index + 1, removedNodes.size());
    }

    /** Expand node. This operation will keep the structure of children(not expand children) */
    void expandNode(TreeNode<D> treeNode) {
        if (treeNode == null) {
            return;
        }
        var additionNodes = TreeHelper.expandNode(treeNode, false);
        var index = expandedNodeList.indexOf(treeNode);

        insertNodesAtIndex(index, additionNodes);
    }

    /** Collapse node. This operation will keep the structure of children(not collapse children) */
    void collapseNode(TreeNode<D> treeNode) {
        if (treeNode == null) {
            return;
        }
        var removedNodes = TreeHelper.collapseNode(treeNode, false);
        var index = expandedNodeList.indexOf(treeNode);

        removeNodesAtIndex(index, removedNodes);
    }

    /** Delete a node from list.This operation will also delete its children. */
    void deleteNode(TreeNode<D> node) {
        if (node == null || node.getParent() == null) {
            return;
        }
        var allNodes = TreeHelper.getAllNodes(root);
        if (allNodes.contains(node)) {
            node.getParent().removeChild(node);
        }

        // remove children form list before delete
        collapseNode(node);

        var index = expandedNodeList.indexOf(node);
        if (index != -1) {
            expandedNodeList.remove(node);
        }
        notifyItemRemoved(index);
    }

    void setTreeView(TreeView<D> treeView) {
        this.treeView = treeView;
    }
}
