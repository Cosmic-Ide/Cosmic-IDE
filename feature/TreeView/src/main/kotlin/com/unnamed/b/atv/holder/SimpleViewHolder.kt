/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package com.unnamed.b.atv.holder

import android.content.Context
import android.view.View
import android.widget.TextView
import com.unnamed.b.atv.model.TreeNode

/** Created by Bogdan Melnychuk on 2/11/15.  */
class SimpleViewHolder(context: Context) : TreeNode.BaseNodeViewHolder<Any?>(context) {
    override fun createNodeView(node: TreeNode?, value: Any?): View {
        val tv = TextView(context)
        tv.text = value.toString()
        return tv
    }

    override fun toggle(active: Boolean) {}
}