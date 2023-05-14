package org.cosmicide.rewrite.editor

import android.content.Context
import android.util.AttributeSet
import android.view.inputmethod.EditorInfo
import com.google.common.collect.ImmutableSet
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.widget.CodeEditor
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
        setFont()
        setInputType(createInputFlags())
        updateNonPrintablePaintingFlags()
        updateTextSize()
        updateTabSize()
    }

    override fun commitText(text: CharSequence?, applyAutoIndent: Boolean) {
        if (text?.length == 1) {
            val currentChar = text.toString().getOrNull(getCursor().left)
            val c = text[0]
            if (ignoredPairEnds.contains(c) && c == currentChar) {
                setSelection(getCursor().leftLine, getCursor().leftColumn + 1)
                return
            }
        }
        super.commitText(text, applyAutoIndent)
    }

    fun appendText(text: String): Int {
        val content = getText()
        if (getLineCount() <= 0) {
            return 0
        }
        var col = content.getColumnCount(getLineCount() - 1)
        if (col < 0) {
            col = 0
        }
        content.insert(getLineCount() - 1, col, text)
        return getLineCount() - 1
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
        setNonPrintablePaintingFlags(
            CodeEditor.FLAG_DRAW_WHITESPACE_LEADING
            or CodeEditor.FLAG_DRAW_WHITESPACE_INNER
            or CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
        )
    }

    private fun createInputFlags(): Int {
        return EditorInfo.TYPE_CLASS_TEXT or
                EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or
                EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
    }
}