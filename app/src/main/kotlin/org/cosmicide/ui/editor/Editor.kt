package org.cosmicide.ui.editor

import android.content.Context
import android.view.inputmethod.EditorInfo
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.toArgb
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
import io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_INNER
import io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_LEADING
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import org.cosmicide.common.Prefs
import org.cosmicide.editor.completion.CustomCompletionItemAdapter
import org.cosmicide.editor.completion.CustomCompletionLayout
import org.cosmicide.editor.language.configureLanguageFor
import org.cosmicide.extension.setFont
import org.cosmicide.project.Project
import java.io.File

data class CodeEditorState(
    var editor: CodeEditor? = null,
    val initialContent: Content = Content()
) {
    var content by mutableStateOf(initialContent)
}

fun setCodeEditorFactory(
    context: Context,
    state: CodeEditorState
): CodeEditor {
    val editor = CodeEditor(context)
    editor.apply {
        setText(state.content)
    }
    state.editor = editor
    return editor
}

fun CodeEditor.applyEditorSettings(project: Project, file: File, theme: ColorScheme) {
    colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
    setTooltipImprovements(theme)
    setFont()

    getComponent(EditorAutoCompletion::class.java).apply {
        setAdapter(CustomCompletionItemAdapter(theme))
        setLayout(CustomCompletionLayout(theme))
    }

    getComponent(EditorDiagnosticTooltipWindow::class.java).apply {
        this.parentView.setBackgroundColor(theme.surfaceContainer.toArgb())
    }

    inputType = EditorInfo.TYPE_CLASS_TEXT or
            EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or
            EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

    val flags = (FLAG_DRAW_WHITESPACE_LEADING
            or FLAG_DRAW_WHITESPACE_INNER
            or FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE)
    nonPrintablePaintingFlags = if (Prefs.nonPrintableCharacters) flags else 0

    setTextSize(Prefs.editorFontSize)
    tabWidth = Prefs.tabSize
    setInterceptParentHorizontalScrollIfNeeded(true)
    isLigatureEnabled = Prefs.useLigatures
    isWordwrap = Prefs.wordWrap
    setScrollBarEnabled(Prefs.scrollbarEnabled)
    isHardwareAcceleratedDrawAllowed = Prefs.hardwareAcceleration
    isLineNumberEnabled = Prefs.lineNumbers
    props.deleteEmptyLineFast = Prefs.quickDelete
    props.stickyScroll = Prefs.stickyScroll
    props.symbolPairAutoCompletion = Prefs.bracketPairAutocomplete

    colorScheme = TextMateColorScheme.create(
        ThemeRegistry.getInstance().currentThemeModel
    )

    configureLanguageFor(project, file)
}

private fun CodeEditor.setTooltipImprovements(colorScheme: ColorScheme) {
    getComponent(EditorDiagnosticTooltipWindow::class.java).apply {
        setSize(500, 100)
        parentView.setBackgroundColor(
            colorScheme.surface.toArgb()
        )
    }
}
