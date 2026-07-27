package org.cosmicide.editor.preview

import android.util.Log
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorPreviewMatchRequest
import org.cosmicide.editor.EditorPreviewProvider
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.project.Project
import java.io.File

object EditorPreviews {
    fun providerFor(project: Project, file: File): EditorPreviewProvider? {
        val request = EditorPreviewMatchRequest(project, file)
        return CosmicPluginHost
            .enabledExtensions(EditorExtensionPoints.PREVIEW_PROVIDER)
            .firstOrNull { provider ->
                runCatching { provider.supports(request) }
                    .onFailure {
                        Log.w(TAG, "Preview provider ${provider.id} failed to match", it)
                    }
                    .getOrDefault(false)
            }
    }

    private const val TAG = "EditorPreviewRouter"
}
