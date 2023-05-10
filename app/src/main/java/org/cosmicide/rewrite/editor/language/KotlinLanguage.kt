package org.cosmicide.rewrite.editor.language

import android.os.Bundle
import android.util.Log
import com.tyron.kotlin.completion.KotlinEnvironment
import io.github.rosemoe.sora.lang.completion.CompletionPublisher
import io.github.rosemoe.sora.langs.textmate.IdeLanguage
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
) : IdeLanguage(
    grammarRegistry.findGrammar("source.kotlin"),
    grammarRegistry.findLanguageConfiguration("source.kotlin"),
    grammarRegistry,
    themeRegistry
) {

    private val kotlinEnvironment: KotlinEnvironment by lazy { KotlinEnvironment.get(project) }
    private var fileName: String = file.name

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

    override fun requireAutoComplete(
        content: ContentReference,
        position: CharPosition,
        publisher: CompletionPublisher,
        extraArguments: Bundle
    ) {
        super.requireAutoComplete(content, position, publisher, extraArguments)

        try {
            val text = editor.text.toString()
            val ktFile = kotlinEnvironment.updateKotlinFile(fileName, text)
            val itemList = ktFile.let {
                kotlinEnvironment.complete(
                    it, position.line, position.column
                )
            }
            publisher.addItems(itemList)
        } catch (e: Throwable) {
            if (e !is InterruptedException) {
                Log.e(TAG, "Failed to fetch code completions", e)
            }
        }
    }

    companion object {
        private const val TAG = "KotlinLanguage"
        private val grammarRegistry = GrammarRegistry.getInstance()
        private val themeRegistry = ThemeRegistry.getInstance()
    }
}