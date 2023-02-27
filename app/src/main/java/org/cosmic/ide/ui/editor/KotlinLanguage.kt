package org.cosmic.ide.ui.editor

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
import org.cosmic.ide.project.Project
import java.io.File

class KotlinLanguage(
        private val mEditor: CodeEditor,
        project: Project,
        file: File
) : TextMateLanguage(
        GrammarRegistry.getInstance().findGrammar("source.kotlin"),
        GrammarRegistry.getInstance().findLanguageConfiguration("source.kotlin"),
        GrammarRegistry.getInstance(),
        ThemeRegistry.getInstance(),
        false
) {

    private val kotlinEnvironment: KotlinEnvironment by lazy { KotlinEnvironment.get(project) }
    private val fileName: String
    private val TAG = "KotlinLanguage"

    init {
        val ktFile = kotlinEnvironment.updateKotlinFile(
                file.absolutePath, mEditor.text.toString())
        fileName = ktFile.name
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
            val ktFile = kotlinEnvironment.updateKotlinFile(fileName, text)
            val itemList = kotlinEnvironment.complete(
                    ktFile, position.line, position.column)
            publisher.addItems(itemList)
        } catch (e: Throwable) {
            if (e !is InterruptedException) {
                Log.e(TAG, "Failed to fetch code suggestions", e)
            }
        }
        super.requireAutoComplete(content, position, publisher, extraArguments)
    }
}
