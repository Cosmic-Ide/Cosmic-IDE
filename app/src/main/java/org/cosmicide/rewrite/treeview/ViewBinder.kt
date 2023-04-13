package org.cosmicide.rewrite.treeview

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Space
import androidx.core.view.updateLayoutParams
import io.github.dingyi222666.view.treeview.DataSource
import io.github.dingyi222666.view.treeview.TreeNode
import io.github.dingyi222666.view.treeview.TreeNodeEventListener
import io.github.dingyi222666.view.treeview.TreeView
import io.github.dingyi222666.view.treeview.TreeViewBinder
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.databinding.TreeviewItemDirBinding
import org.cosmicide.rewrite.databinding.TreeviewItemFileBinding


class ViewBinder(var layoutInflater: LayoutInflater) : TreeViewBinder<DataSource<String>>(),
    TreeNodeEventListener<DataSource<String>> {

    override fun createView(parent: ViewGroup, viewType: Int): View {
        return if (viewType == 1) {
            TreeviewItemDirBinding.inflate(layoutInflater, parent, false).root
        } else {
            TreeviewItemFileBinding.inflate(layoutInflater, parent, false).root
        }
    }

    override fun getItemViewType(node: TreeNode<DataSource<String>>): Int {
        if (node.isChild) {
            return 1
        }
        return 0
    }

    override fun bindView(
        holder: TreeView.ViewHolder,
        node: TreeNode<DataSource<String>>,
        listener: TreeNodeEventListener<DataSource<String>>
    ) {
        if (node.isChild) {
            applyDir(holder, node)
        } else {
            applyFile(holder, node)
        }

        val itemView = holder.itemView.findViewById<Space>(R.id.space)

        itemView.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            width = node.depth * 22.dp
        }

    }

    private fun applyFile(holder: TreeView.ViewHolder, node: TreeNode<DataSource<String>>) {
        val binding = TreeviewItemFileBinding.bind(holder.itemView)
        binding.textView.text = node.name.toString()
    }

    private fun applyDir(holder: TreeView.ViewHolder, node: TreeNode<DataSource<String>>) {
        val binding = TreeviewItemDirBinding.bind(holder.itemView)
        binding.textView.text = node.name.toString()

        binding
            .imageView
            .animate()
            .rotation(if (node.expand) 90f else 0f)
            .setDuration(200)
            .start()
    }


    override fun onClick(node: TreeNode<DataSource<String>>, holder: TreeView.ViewHolder) {
        if (node.isChild) {
            applyDir(holder, node)
        }
    }

    override fun onToggle(
        node: TreeNode<DataSource<String>>,
        isExpand: Boolean,
        holder: TreeView.ViewHolder
    ) {
        applyDir(holder, node)
    }
}

inline val Int.dp: Int
    get() = (Resources.getSystem().displayMetrics.density * this + 0.5f).toInt()
