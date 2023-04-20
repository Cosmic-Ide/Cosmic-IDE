package org.cosmicide.rewrite.editor

import android.os.Bundle
import android.util.Log
import androidx.annotation.WorkerThread
import com.tyron.javacompletion.JavaCompletions
import com.tyron.javacompletion.options.JavaCompletionOptionsImpl
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandleResult
import io.github.rosemoe.sora.lang.smartEnter.NewlineHandler
import io.github.rosemoe.sora.lang.styling.Styles
import io.github.rosemoe.sora.lang.styling.StylesUtils
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.text.TextUtils
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmicide.project.Project
import org.cosmicide.rewrite.common.Prefs
import java.io.File
import java.lang.Character.isWhitespace
import java.net.URI
import java.nio.file.Path
import java.util.logging.Level


class JavaLanguage(
    val editor: CodeEditor,
    val project: Project,
    val file: File
) : TextMateLanguage(
    grammarRegistry.findGrammar("source.java"),
    grammarRegistry.findLanguageConfiguration("source.java"),
    grammarRegistry,
    themeRegistry,
    false
) {

    private val completions: JavaCompletions by lazy { JavaCompletions() }
    private val path: Path = file.toPath()

    init {
        tabSize = Prefs.tabSize
        useTab(!Prefs.useSpaces)
        val options = JavaCompletionOptionsImpl(
            "${project.binDir.absolutePath}${File.separator}autocomplete.log",
            Level.ALL,
            emptyList(),
            emptyList()
        )
        completions.initialize(URI("file://${project.root.absolutePath}"), options)
    }

    @WorkerThread
    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        try {
            val text = editor.text.toString()
            completions.updateFileContent(path, text)
            val result = completions.getCompletions(path, position.line, position.column)
            result.completionCandidates.forEach { candidate ->
                if (candidate.name != "<error>") {
                    val item = SimpleCompletionItem(
                        candidate.name,
                        candidate.detail.orElse("Unknown"),
                        result.prefix.length,
                        candidate.name
                    )
                    publisher.addItem(item)
                }
            }
        } catch (e: Throwable) {
            if (e !is InterruptedException) {
                Log.e(TAG, "Failed to fetch code suggestions", e)
            }
        }
        super.requireAutoComplete(content, position, publisher, extraArguments)
    }


    private val newLineHandlers = arrayOf<NewlineHandler>(
        BraceHandler()
    )

    override fun getNewlineHandlers(): Array<NewlineHandler> {
        return newLineHandlers
    }

    class BraceHandler : NewlineHandler {

        override fun matchesRequirement(
            text: Content,
            position: CharPosition,
            style: Styles?
        ): Boolean {
            val line = text.getLine(position.line - 1)
            return !StylesUtils.checkNoCompletion(style, position) && getNonEmptyTextBefore(
                line,
                position.column,
                1
            ).equals("{") &&
                    getNonEmptyTextAfter(line, position.column, 1).equals("}")
        }

        override fun handleNewline(
            text: Content,
            position: CharPosition,
            style: Styles?,
            tabSize: Int
        ): NewlineHandleResult {
            val line = text.getLine(position.line)
            val index = position.column
            val beforeText = line.subSequence(0, index).toString()
            val afterText = line.subSequence(index, line.length).toString()
            return handleNewline(beforeText, afterText, tabSize)
        }

        fun handleNewline(
            beforeText: String,
            afterText: String,
            tabSize: Int
        ): NewlineHandleResult {
            val count = TextUtils.countLeadingSpaceCount(beforeText, tabSize)
            val advanceBefore = getIndentAdvance(beforeText)
            val advanceAfter = getIndentAdvance(afterText)
            val sb = StringBuilder("\n")
                .append(TextUtils.createIndent(count, tabSize, (!Prefs.useSpaces)))
                .append('\n')
                .append(TextUtils.createIndent(count, tabSize, (!Prefs.useSpaces)))
            val shiftLeft = sb.length + 1
            return NewlineHandleResult(sb, shiftLeft)
        }

        private fun getNonEmptyTextBefore(text: CharSequence, index: Int, length: Int): String {
            var index = index
            while (index > 0 && isWhitespace(text[index - 1])) {
                index--
            }
            return text.subSequence(Math.max(0, index - length), index).toString()
        }

        private fun getNonEmptyTextAfter(text: CharSequence, index: Int, length: Int): String {
            var index = index
            while (index < text.length && isWhitespace(text[index])) {
                index++
            }
            return text.subSequence(index, (index + length).coerceAtMost(text.length)).toString()
        }

        private fun getIndentAdvance(content: String): Int {
            var advance = 0
            for (c in content) {
                if (c == '{')
                    advance++
            }
            advance = 0.coerceAtLeast(advance)
            return advance * 4
        }
    }

    override fun destroy() {
        super.destroy()
        completions.shutdown()
    }

    companion object {
        private const val TAG = "JavaLanguage"
        private val grammarRegistry = GrammarRegistry.getInstance()
        private val themeRegistry = ThemeRegistry.getInstance()
    }
}