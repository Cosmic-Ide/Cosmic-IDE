/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */
package com.unnamed.b.atv.view

import android.annotation.SuppressLint
import android.content.Context
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import com.unnamed.b.atv.R

/** Created by Bogdan Melnychuk on 2/10/15.  */
@SuppressLint("ViewConstructor")
class TreeNodeWrapperView(context: Context?, private val containerStyle: Int) :
    LinearLayout(context) {
    private var nodeItemsContainer: LinearLayout? = null
    var nodeContainer: ViewGroup? = null
        private set

    init {
        init()
    }

    private fun init() {
        orientation = VERTICAL
        nodeContainer = RelativeLayout(context)
        (nodeContainer as RelativeLayout).layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        (nodeContainer as RelativeLayout).id = R.id.node_header
        val newContext = ContextThemeWrapper(
            context,
            containerStyle
        )
        nodeItemsContainer = LinearLayout(newContext, null, containerStyle)
        nodeItemsContainer!!.layoutParams =
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        nodeItemsContainer!!.id = R.id.node_items
        nodeItemsContainer!!.orientation = VERTICAL
        nodeItemsContainer!!.visibility = GONE
        addView(nodeContainer)
        addView(nodeItemsContainer)
    }

    fun insertNodeView(nodeView: View?) {
        nodeContainer!!.addView(nodeView)
    }
}