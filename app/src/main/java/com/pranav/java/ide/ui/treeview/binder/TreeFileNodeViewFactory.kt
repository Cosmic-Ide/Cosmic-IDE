package com.pranav.java.ide.ui.treeview.binder

import android.view.View
import com.pranav.java.ide.R
import com.pranav.java.ide.ui.treeview.base.BaseNodeViewBinder
import com.pranav.java.ide.ui.treeview.base.BaseNodeViewFactory
import com.pranav.java.ide.ui.treeview.binder.TreeFileNodeViewBinder.TreeFileNodeListener
import com.pranav.java.ide.ui.treeview.file.TreeFile

class TreeFileNodeViewFactory(
    private var nodeListener: TreeFileNodeListener
): BaseNodeViewFactory<TreeFile>() {

    override fun getNodeViewBinder(view: View, level: Int) = TreeFileNodeViewBinder(view, level, nodeListener)

    override fun getNodeLayoutId(level: Int) = R.layout.treeview_item

}