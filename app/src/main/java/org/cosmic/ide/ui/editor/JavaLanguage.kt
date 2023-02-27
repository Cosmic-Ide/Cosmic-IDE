package org.cosmic.ide.ui.editor

import android.os.Bundle
import android.util.Log
import androidx.annotation.WorkerThread
import com.tyron.javacompletion.JavaCompletions
import com.tyron.javacompletion.options.JavaCompletionOptionsImpl
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.lang.completion.SimpleCompletionItem
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmic.ide.project.Project
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.util.logging.Level

class JavaLanguage(
        private val mEditor: CodeEditor,
        project: Project,
        file: File
) : TextMateLanguage(
        GrammarRegistry.getInstance().findGrammar("source.java"),
        GrammarRegistry.getInstance().findLanguageConfiguration("source.java"),
        GrammarRegistry.getInstance(),
        ThemeRegistry.getInstance(),
        false
        ) {

private val completions: JavaCompletions by lazy { JavaCompletions() }
private val path: Path
private val TAG = "JavaLanguage"

        init {
                val options = JavaCompletionOptionsImpl(project.binDirPath + "autocomplete.log",
                                     Level.ALL,
                                     emptyList(),
                                     emptyList())
                 path = file.toPath()
                completions.initialize(URI("file://" + project.projectDirPath), options)
        }

@WorkerThread
    override fun requireAutoComplete(
            content: ContentReference,
            position: CharPosition,
            publisher: CompletionPublisher,
            extraArguments: Bundle
            ) {
            try {
            val text = mEditor.text.toString()
                    Files.write(path, text.toByteArray())
            val result = completions.project.getCompletionResult(path, position.line, position.column)
            result.completionCandidates.forEach {
                    if (it.name != "<error>") {
                    val item = SimpleCompletionItem(it.name, it.detail.orElse("Unknown"), result.prefix.length, it.name)
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
            }
