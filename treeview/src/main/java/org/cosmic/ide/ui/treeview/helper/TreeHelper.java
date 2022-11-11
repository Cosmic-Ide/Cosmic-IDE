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
package org.cosmic.ide.ui.treeview.helper;

import org.cosmic.ide.ui.treeview.TreeNode;

import java.util.ArrayList;
import java.util.List;

/** Created by xinyuanzhong on 2017/4/27. */
public class TreeHelper {

    public static <D> void expandAll(TreeNode<D> node) {
        if (node == null) {
            return;
        }
        expandNode(node, true);
    }

    /**
     * Expand node and calculate the visible addition nodes.
     *
     * @param treeNode target node to expand
     * @param includeChild should expand child
     * @return the visible addition nodes
     */
    public static <D> List<TreeNode<D>> expandNode(TreeNode<D> treeNode, boolean includeChild) {
        var expandChildren = new ArrayList<TreeNode<D>>();

        if (treeNode == null) {
            return expandChildren;
        }

        treeNode.setExpanded(true);

        if (!treeNode.hasChild()) {
            return expandChildren;
        }

        for (var child : treeNode.getChildren()) {
            expandChildren.add(child);

            if (includeChild || child.isExpanded()) {
                expandChildren.addAll(expandNode(child, includeChild));
            }
        }

        return expandChildren;
    }

    /**
     * Expand the same deep(level) nodes.
     *
     * @param root the tree root
     * @param level the level to expand
     */
    public static <D> void expandLevel(TreeNode<D> root, int level) {
        if (root == null) {
            return;
        }

        for (var child : root.getChildren()) {
            if (child.getLevel() == level) {
                expandNode(child, false);
            } else {
                expandLevel(child, level);
            }
        }
    }

    public static <D> void collapseAll(TreeNode<D> node) {
        if (node == null) {
            return;
        }
        for (var child : node.getChildren()) {
            performCollapseNode(child, true);
        }
    }

    /**
     * Collapse node and calculate the visible removed nodes.
     *
     * @param node target node to collapse
     * @param includeChild should collapse child
     * @return the visible addition nodes before remove
     */
    public static <D> List<TreeNode<D>> collapseNode(TreeNode<D> node, boolean includeChild) {
        var treeNodes = performCollapseNode(node, includeChild);
        node.setExpanded(false);
        return treeNodes;
    }

    private static <D> List<TreeNode<D>> performCollapseNode(
            TreeNode<D> node, boolean includeChild) {
        var collapseChildren = new ArrayList<TreeNode<D>>();

        if (node == null) {
            return collapseChildren;
        }
        if (includeChild) {
            node.setExpanded(false);
        }
        for (var child : node.getChildren()) {
            collapseChildren.add(child);

            if (child.isExpanded()) {
                collapseChildren.addAll(performCollapseNode(child, includeChild));
            } else if (includeChild) {
                performCollapseNodeInner(child);
            }
        }

        return collapseChildren;
    }

    /**
     * Collapse all children node recursive
     *
     * @param node target node to collapse
     */
    private static <D> void performCollapseNodeInner(TreeNode<D> node) {
        if (node == null) {
            return;
        }
        node.setExpanded(false);
        for (var child : node.getChildren()) {
            performCollapseNodeInner(child);
        }
    }

    public static <D> void collapseLevel(TreeNode<D> root, int level) {
        if (root == null) {
            return;
        }
        for (var child : root.getChildren()) {
            if (child.getLevel() == level) {
                collapseNode(child, false);
            } else {
                collapseLevel(child, level);
            }
        }
    }

    public static <D> List<TreeNode<D>> getAllNodes(TreeNode<D> root) {
        var allNodes = new ArrayList<TreeNode<D>>();

        fillNodeList(allNodes, root);
        allNodes.remove(root);

        return allNodes;
    }

    private static <D> void fillNodeList(List<TreeNode<D>> treeNodes, TreeNode<D> treeNode) {
        treeNodes.add(treeNode);

        if (treeNode.hasChild()) {
            for (var child : treeNode.getChildren()) {
                fillNodeList(treeNodes, child);
            }
        }
    }

    /** Select the node and node's children,return the visible nodes */
    public static <D> List<TreeNode<D>> selectNodeAndChild(TreeNode<D> treeNode, boolean select) {
        var expandChildren = new ArrayList<TreeNode<D>>();

        if (treeNode == null) {
            return expandChildren;
        }

        treeNode.setSelected(select);

        if (!treeNode.hasChild()) {
            return expandChildren;
        }

        if (treeNode.isExpanded()) {
            for (var child : treeNode.getChildren()) {
                expandChildren.add(child);

                if (child.isExpanded()) {
                    expandChildren.addAll(selectNodeAndChild(child, select));
                } else {
                    selectNodeInner(child, select);
                }
            }
        } else {
            selectNodeInner(treeNode, select);
        }
        return expandChildren;
    }

    private static <D> void selectNodeInner(TreeNode<D> treeNode, boolean select) {
        if (treeNode == null) {
            return;
        }
        treeNode.setSelected(select);
        if (treeNode.hasChild()) {
            for (var child : treeNode.getChildren()) {
                selectNodeInner(child, select);
            }
        }
    }

    /**
     * Select parent when all the brothers have been selected, otherwise deselect parent, and check
     * the grand parent recursive.
     */
    public static <D> List<TreeNode<D>> selectParentIfNeedWhenNodeSelected(
            TreeNode<D> treeNode, boolean select) {
        var impactedParents = new ArrayList<TreeNode<D>>();
        if (treeNode == null) {
            return impactedParents;
        }

        // ensure that the node's level is bigger than 1(first level is 1)
        var parent = treeNode.getParent();
        if (parent == null || parent.getParent() == null) {
            return impactedParents;
        }

        var brothers = parent.getChildren();
        int selectedBrotherCount = 0;
        for (var brother : brothers) {
            if (brother.isSelected()) selectedBrotherCount++;
        }

        if (select && selectedBrotherCount == brothers.size()) {
            parent.setSelected(true);
            impactedParents.add(parent);
            impactedParents.addAll(selectParentIfNeedWhenNodeSelected(parent, true));
        } else if (!select && selectedBrotherCount == brothers.size() - 1) {
            // only the condition that the size of selected's brothers
            // is one less than total count can trigger the deselect
            parent.setSelected(false);
            impactedParents.add(parent);
            impactedParents.addAll(selectParentIfNeedWhenNodeSelected(parent, false));
        }
        return impactedParents;
    }

    /** Get the selected nodes under current node, include itself */
    public static <D> List<TreeNode<D>> getSelectedNodes(TreeNode<D> treeNode) {
        var selectedNodes = new ArrayList<TreeNode<D>>();
        if (treeNode == null) {
            return selectedNodes;
        }

        if (treeNode.isSelected() && treeNode.getParent() != null) selectedNodes.add(treeNode);

        for (var child : treeNode.getChildren()) {
            selectedNodes.addAll(getSelectedNodes(child));
        }
        return selectedNodes;
    }

    /**
     * Return true when the node has one selected child(recurse all children) at least, otherwise
     * return false
     */
    public static <D> boolean hasOneSelectedNodeAtLeast(TreeNode<D> treeNode) {
        if (treeNode == null || treeNode.getChildren().size() == 0) {
            return false;
        }
        var children = treeNode.getChildren();
        for (var child : children) {
            if (child.isSelected() || hasOneSelectedNodeAtLeast(child)) {
                return true;
            }
        }
        return false;
    }
}
