/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.editor

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import com.google.common.collect.ImmutableSet
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.cosmicide.rewrite.common.Prefs
import org.cosmicide.rewrite.extension.setCompletionLayout
import org.cosmicide.rewrite.extension.setFont

class IdeEditor @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : CodeEditor(context, attrs, defStyleAttr, defStyleRes) {

    private val ignoredPairEnds: Set<Char> = ImmutableSet.of(
        ')', ']', '}', '"', '>', '\'', ';'
    )

    init {
        colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
        setCompletionLayout()
        setTooltipImprovements()
        setFont()
        inputType = createInputFlags()
        updateNonPrintablePaintingFlags()
        updateTextSize()
        updateTabSize()
        setInterceptParentHorizontalScrollIfNeeded(true)
        isLigatureEnabled = Prefs.useLigatures
        isWordwrap = Prefs.wordWrap
        setScrollBarEnabled(Prefs.scrollbarEnabled)
        isHardwareAcceleratedDrawAllowed = Prefs.hardwareAcceleration
        isLineNumberEnabled = Prefs.lineNumbers
        props.deleteEmptyLineFast = Prefs.quickDelete
        props.stickyScroll = Prefs.stickyScroll
    }

    override fun commitText(text: CharSequence?, applyAutoIndent: Boolean) {
        if (text?.length == 1) {
            val currentChar = text.toString().getOrNull(cursor.left)
            val c = text[0]
            if (ignoredPairEnds.contains(c) && c == currentChar) {
                setSelection(cursor.leftLine, cursor.leftColumn + 1)
                return
            }
        }
        super.commitText(text, applyAutoIndent)
    }

    fun appendText(text: String): Int {
        val content = getText()
        if (lineCount <= 0) {
            return 0
        }
        var col = content.getColumnCount(lineCount - 1)
        if (col < 0) {
            col = 0
        }
        content.insert(lineCount - 1, col, text)
        return lineCount - 1
    }

    private fun updateTextSize() {
        val textSize = Prefs.editorFontSize
        setTextSize(textSize)
    }

    private fun updateTabSize() {
        val tabSize = Prefs.tabSize
        tabWidth = tabSize
    }

    private fun updateNonPrintablePaintingFlags() {
        val flags = (FLAG_DRAW_WHITESPACE_LEADING
                or FLAG_DRAW_WHITESPACE_INNER
                or FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE)
        nonPrintablePaintingFlags = if (Prefs.nonPrintableCharacters) flags else 0
    }

    private fun createInputFlags(): Int {
        return EditorInfo.TYPE_CLASS_TEXT or
                EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or
                EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }

    private fun setTooltipImprovements() {
        getComponent(EditorDiagnosticTooltipWindow::class.java).apply {
            setSize(500, 100)
            parentView.setBackgroundColor(colorScheme.getColor(EditorColorScheme.WHOLE_BACKGROUND))
        }
    }
}
