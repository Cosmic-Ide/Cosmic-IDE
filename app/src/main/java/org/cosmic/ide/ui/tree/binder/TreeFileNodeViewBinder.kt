package org.cosmic.ide.ui.treeview.binder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.ViewCompat
import org.cosmic.ide.R
import org.cosmic.ide.ui.treeview.TreeNode
import org.cosmic.ide.ui.treeview.base.BaseNodeViewBinder
import org.cosmic.ide.ui.treeview.model.TreeFile
import org.cosmic.ide.util.AndroidUtilities

class TreeFileNodeViewBinder(
    itemView: View,
    private val level: Int,
    private val nodeListener: TreeFileNodeListener
) : BaseNodeViewBinder<TreeFile>(itemView) {

    private lateinit var viewHolder: ViewHolder

    override fun bindView(treeNode: TreeNode<TreeFile>) {
        viewHolder = ViewHolder(itemView)

        val isRightToLeft = ViewCompat.getLayoutDirection(viewHolder.rootView) == ViewCompat.LAYOUT_DIRECTION_RTL
        if (isRightToLeft) {
            AndroidUtilities.setMargins(viewHolder.rootView, 0, 0, level * AndroidUtilities.dpToPx(12f), 0)
        } else {
            AndroidUtilities.setMargins(viewHolder.rootView, level * AndroidUtilities.dpToPx(12f), 0, 0, 0)
        }

        with(viewHolder.arrow) {
            setImageResource(R.drawable.arrow_forward)
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

    override fun onNodeLongClicked(view: View, treeNode: TreeNode<TreeFile>, expanded: Boolean): Boolean {
        return nodeListener.onNodeLongClicked(view, treeNode, expanded)
    }

    class ViewHolder(val rootView: View) {
        val arrow: ImageView = rootView.findViewById(R.id.arrow)
        val icon: ImageView = rootView.findViewById(R.id.icon)
        val dirName: TextView = rootView.findViewById(R.id.name)
    }

    interface TreeFileNodeListener {
        fun onNodeToggled(treeNode: TreeNode<TreeFile>?, expanded: Boolean)
        fun onNodeLongClicked(view: View?, treeNode: TreeNode<TreeFile>?, expanded: Boolean): Boolean
    }
}
