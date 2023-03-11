package org.cosmicide.rewrite.editor

import android.os.Bundle
import android.util.Log
import androidx.annotation.WorkerThread
import com.tyron.kotlin.completion.KotlinEnvironment
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.text.CharPosition
import io.github.rosemoe.sora.text.ContentReference
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmicide.project.Project
import java.io.File

class KotlinLanguage(
    private val editor: CodeEditor,
    private val project: Project,
    private val file: File
) : TextMateLanguage(
    grammarRegistry.findGrammar("source.kotlin"),
    grammarRegistry.findLanguageConfiguration("source.kotlin"),
    grammarRegistry,
    themeRegistry,
    false
) {

    private val kotlinEnvironment: KotlinEnvironment by lazy { KotlinEnvironment.get(project) }
    private var fileName = file.name

    init {
        try {
            val ktFile = kotlinEnvironment.updateKotlinFile(
                file.absolutePath, editor.text.toString()
            )
            fileName = ktFile.name
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update Kotlin file", e)
        }
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
            val ktFile = kotlinEnvironment.updateKotlinFile(fileName, text)
            val itemList = kotlinEnvironment.complete(
                ktFile, position.line, position.column
            )
            publisher.addItems(itemList)
        } catch (e: Throwable) {
            if (e !is InterruptedException) {
                Log.e(TAG, "Failed to fetch code suggestions", e)
            }
        }
        super.requireAutoComplete(content, position, publisher, extraArguments)
    }

    companion object {
        private const val TAG = "KotlinLanguage"
        private val grammarRegistry = GrammarRegistry.getInstance()
        private val themeRegistry = ThemeRegistry.getInstance()
    }
}