/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.res.ResourcesCompat
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

    @SuppressLint("SetTextI18n")
    override fun getView(
        position: Int,
        convertView: android.view.View?,
        parent: ViewGroup
    ): android.view.View {
        val binding = convertView?.let {
            NavItemBinding.bind(it)
        } ?: NavItemBinding.inflate(LayoutInflater.from(context), parent, false)
        val item = items[position]
        val pos = indexer.getCharPosition(item.startPosition)
        val startLine = pos.line + 1
        val startColumn = pos.column + 1
        binding.navItemTitle.text = item.name
        binding.description.text =
            "${item.modifiers} ($startLine:$startColumn)"

        binding.navItemTitle.typeface = ResourcesCompat.getFont(context, R.font.noto_sans_mono)
        binding.description.typeface = ResourcesCompat.getFont(context, R.font.noto_sans_mono)

        val type = when (item) {
            is NavigationProvider.MethodNavigationItem -> CompletionItemKind.Method
            is NavigationProvider.FieldNavigationItem -> CompletionItemKind.Field
            else -> CompletionItemKind.Class
        }
        binding.space.layoutParams.width = item.depth * 30
        binding.navItemIcon
            .setImageDrawable(SimpleCompletionIconDrawer.draw(type))
        return binding.root
    }
}
