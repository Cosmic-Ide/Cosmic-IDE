package org.cosmic.ide.ui.treeview.binder

import android.view.View
import org.cosmic.ide.R
import org.cosmic.ide.ui.treeview.base.BaseNodeViewFactory
import org.cosmic.ide.ui.treeview.binder.TreeFileNodeViewBinder.TreeFileNodeListener
import org.cosmic.ide.ui.treeview.model.TreeFile

class TreeFileNodeViewFactory(
    private var nodeListener: TreeFileNodeListener
) : BaseNodeViewFactory<TreeFile>() {

    override fun getNodeViewBinder(view: View, level: Int) = TreeFileNodeViewBinder(view, level, nodeListener)

    override fun getNodeLayoutId(level: Int) = R.layout.file_manager_item
}
