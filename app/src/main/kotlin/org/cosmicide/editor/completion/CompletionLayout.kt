/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.editor.completion

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.ProgressBar
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.drawable.toDrawable
import io.github.rosemoe.sora.widget.component.DefaultCompletionLayout
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class CustomCompletionLayout(
    private val materialColors: ColorScheme
) : DefaultCompletionLayout() {

    override fun inflate(context: Context): View {
        return super.inflate(context).also {
            completionList.apply {
                selector = Color.TRANSPARENT.toDrawable()
                cacheColorHint = Color.TRANSPARENT
                overScrollMode = View.OVER_SCROLL_NEVER
                isVerticalScrollBarEnabled = true
            }
        }
    }

    override fun onApplyColorScheme(colorScheme: EditorColorScheme) {
        super.onApplyColorScheme(colorScheme)

        val container = completionList.parent as View
        val radius = container.dp(14f)
        container.background = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = radius
            setColor(materialColors.surfaceContainer.toArgb())
            setStroke(
                container.dp(1f).toInt(),
                materialColors.outlineVariant.copy(alpha = 0.72f).toArgb()
            )
        }
        container.elevation = container.dp(10f)
        container.clipToOutline = true
        container.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(view: View, outline: Outline) {
                outline.setRoundRect(0, 0, view.width, view.height, radius)
            }
        }

        completionList.setBackgroundColor(Color.TRANSPARENT)
        ((container as? ViewGroup)?.getChildAt(0) as? ProgressBar)?.apply {
            indeterminateTintList = ColorStateList.valueOf(materialColors.primary.toArgb())
            progressTintList = ColorStateList.valueOf(materialColors.primary.toArgb())
        }
    }

    private fun View.dp(value: Float): Float {
        return value * resources.displayMetrics.density
    }
}
