package org.cosmicide.editor.language

import android.util.Log
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmicide.ide.editor.EditorExtensionPoints
import org.cosmicide.ide.editor.EditorLanguageRequest
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

    val configured = CosmicPluginHost.extensionRegistry
        .extensions(EditorExtensionPoints.LANGUAGE_PROVIDER)
        .asSequence()
        .filter { it.supports(request) }
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
