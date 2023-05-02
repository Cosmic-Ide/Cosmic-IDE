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
