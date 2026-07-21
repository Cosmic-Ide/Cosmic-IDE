package org.cosmicide.editor.language

import android.util.Log
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorLanguageRequest
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.util.ProjectHandler
import java.io.File

private const val TAG = "EditorLanguageRouter"

fun CodeEditor.configureLanguageFor(file: File) {
    val project = ProjectHandler.getProject() ?: return
    val request = EditorLanguageRequest(
        editor = this,
        project = project,
        file = file
    )

    val configured = CosmicPluginHost
        .enabledExtensions(EditorExtensionPoints.LANGUAGE_PROVIDER)
        .asSequence()
        .filter { provider ->
            runCatching { provider.supports(request) }
                .onFailure { Log.w(TAG, "Language provider ${provider.id} failed to match", it) }
                .getOrDefault(false)
        }
        .any { provider ->
            runCatching {
                provider.configure(request)
            }.onFailure {
                Log.w(TAG, "Language provider ${provider.id} failed", it)
            }.getOrDefault(false)
        }

    if (!configured) {
        setEditorLanguage(EmptyLanguage())
    }
}
