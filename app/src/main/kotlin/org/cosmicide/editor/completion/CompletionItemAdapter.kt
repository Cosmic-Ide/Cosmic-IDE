/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.completion

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import org.cosmicide.databinding.CompletionResultBinding
import org.cosmicide.rewrite.editor.EditorCompletionItem

class CustomCompletionItemAdapter : EditorCompletionAdapter() {

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun getItemHeight(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            60f,
            context.resources.displayMetrics
        ).toInt()
    }

    override fun getItem(position: Int): EditorCompletionItem {
        return super.getItem(position) as EditorCompletionItem
    }

    override fun getView(
        pos: Int,
        v: View?,
        parent: ViewGroup,
        isCurrentCursorPosition: Boolean
    ): View {
        val binding =
            v?.let { CompletionResultBinding.bind(it) }
                ?: CompletionResultBinding.inflate(LayoutInflater.from(context), parent, false)
        val item = getItem(pos)
        binding.resultItemLabel.text = item.label
        binding.resultItemDesc.text = item.desc
        binding.resultItemIcon.setImageDrawable(item.icon)
        return binding.root
    }
}