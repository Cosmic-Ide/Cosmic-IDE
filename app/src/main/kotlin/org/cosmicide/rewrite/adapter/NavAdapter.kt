/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Space
import android.widget.TextView
import dev.pranav.navigation.NavigationProvider
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lang.completion.SimpleCompletionIconDrawer
import io.github.rosemoe.sora.text.Indexer
import org.cosmicide.rewrite.R
import org.cosmicide.rewrite.databinding.NavItemBinding

class NavAdapter(
    context: Context,
    private val items: List<NavigationProvider.NavigationItem>,
    private val indexer: Indexer
) : ArrayAdapter<NavigationProvider.NavigationItem>(context, R.layout.nav_item, items) {

    override fun getView(
        position: Int,
        convertView: android.view.View?,
        parent: ViewGroup
    ): android.view.View {
        val view =
            convertView ?: NavItemBinding.inflate(LayoutInflater.from(context), parent, false).root
        val item = items[position]
        val textView = view.findViewById<TextView>(R.id.nav_item_title)
        val pos = indexer.getCharPosition(item.startPosition)
        val startLine = pos.line + 1
        val startColumn = pos.column + 1
        textView.text = item.name
        view.findViewById<TextView>(R.id.description).text =
            "${item.modifiers} ($startLine:$startColumn)"


        val type = when (item) {
            is NavigationProvider.MethodNavigationItem -> CompletionItemKind.Method
            is NavigationProvider.FieldNavigationItem -> CompletionItemKind.Field
            else -> CompletionItemKind.Class
        }
        view.findViewById<Space>(R.id.space).layoutParams.width = item.depth * 50
        view.findViewById<ImageView>(R.id.nav_item_icon)
            .setImageDrawable(SimpleCompletionIconDrawer.draw(type))
        return view
    }
}
