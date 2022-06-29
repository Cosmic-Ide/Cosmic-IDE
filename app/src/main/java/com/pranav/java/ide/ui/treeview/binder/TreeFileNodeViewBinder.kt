package com.pranav.java.ide.ui.treeview.binder

import android.graphics.PorterDuff
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.pranav.java.ide.R
import com.pranav.java.ide.ui.treeview.TreeNode
import com.pranav.java.ide.ui.treeview.base.BaseNodeViewBinder
import com.pranav.java.ide.ui.treeview.file.TreeFile
import com.pranav.java.ide.ui.utils.dpToPx
import com.pranav.java.ide.ui.utils.setMargins

class TreeFileNodeViewBinder(
    itemView: View,
    private val level: Int,
    private val nodeListener: TreeFileNodeListener
): BaseNodeViewBinder<TreeFile>(itemView) {

    private lateinit var viewHolder: ViewHolder

    fun bnd(): BaseNodeViewBinder<TreeFile> = this

    override fun bindView(treeNode: TreeNode<TreeFile>) {
        viewHolder = ViewHolder(itemView)

        viewHolder.rootView.setMargins(
            left = level * dpToPx.dpToPx(15f)
        )

        with(viewHolder.arrow) {
            setImageResource(R.drawable.arrow)
            rotation = if (treeNode.isExpanded) 90F else 0F
            visibility = if (treeNode.isLeaf) View.INVISIBLE else View.VISIBLE
        }

        val file = treeNode.content.getFile()

        viewHolder.dirName.text = file.name

        with(viewHolder.icon) {
            setImageDrawable(treeNode.content.getIcon(context))
        }
    }

    override fun onNodeToggled(treeNode: TreeNode<TreeFile>, expand: Boolean) {
        viewHolder.arrow.animate()
            .rotation(if (expand) 90F else 0F)
            .setDuration(150)
            .start()

        nodeListener.onNodeToggled(treeNode, expand)
    }

    override fun onNodeClicked(view: View, treeNode: TreeNode<TreeFile>) {
        return nodeListener.onNodeClicked(view, treeNode)
    }

    override fun onNodeLongClicked(view: View, treeNode: TreeNode<TreeFile>, expanded: Boolean): Boolean {
        return nodeListener.onNodeLongClicked(view, treeNode, expanded)
    }

    class ViewHolder(val rootView: View) {
        val arrow: ImageView = rootView.findViewById(R.id.arrow)
        val icon: ImageView = rootView.findViewById(R.id.icon)
        val dirName: TextView = rootView.findViewById(R.id.name)
    }

    interface TreeFileNodeListener {
        fun onNodeClicked(view: View?, treeNode: TreeNode<TreeFile>?)
        fun onNodeToggled(treeNode: TreeNode<TreeFile>?, expanded: Boolean)
        fun onNodeLongClicked(view: View?, treeNode: TreeNode<TreeFile>?, expanded: Boolean): Boolean
    }

}