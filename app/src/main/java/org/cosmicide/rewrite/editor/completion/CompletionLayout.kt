/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor.completion

import android.content.res.Resources
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import io.github.rosemoe.sora.widget.component.DefaultCompletionLayout
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class CustomCompletionLayout : DefaultCompletionLayout() {

    override fun onApplyColorScheme(colorScheme: EditorColorScheme) {
        super.onApplyColorScheme(colorScheme)

        val completionListParent = completionList.parent as? ViewGroup
            ?: throw IllegalArgumentException("Completion list parent view is null")

        val backgroundDrawable = completionListParent.background as? GradientDrawable
            ?: throw IllegalArgumentException("Completion list parent view background is null or not a GradientDrawable")

        backgroundDrawable.setStroke(1f.dpToPx(), requireNotNull(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER)))
    }

    fun Float.dpToPx(): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (this * scale + 0.5f).toInt()
    }
}
