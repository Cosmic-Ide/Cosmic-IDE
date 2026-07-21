package org.cosmicide.editor.formatter

import android.util.Log
import io.github.rosemoe.sora.lang.format.AsyncFormatter
import io.github.rosemoe.sora.lang.format.Formatter
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.text.TextRange
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorFormatterRequest
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.project.Project
import java.io.File

object EditorFormatters {
    fun createFormatter(project: Project, file: File): Formatter {
        return RegistryBackedFormatter(project, file)
    }
}

private class RegistryBackedFormatter(
    private val project: Project,
    private val file: File
) : AsyncFormatter() {

    override fun formatAsync(text: Content, range: TextRange): TextRange? {
        val originalText = text.toString()
        val request = EditorFormatterRequest(
            project = project,
            file = file,
            text = originalText,
            range = range
        )

        val result = CosmicPluginHost
            .enabledExtensions(EditorExtensionPoints.FORMATTER_PROVIDER)
            .asSequence()
            .filter { provider ->
                runCatching { provider.supports(request) }
                    .onFailure {
                        Log.w(
                            TAG,
                            "Formatter provider ${provider.id} failed to match",
                            it
                        )
                    }
                    .getOrDefault(false)
            }
            .firstNotNullOfOrNull { provider ->
                runCatching {
                    provider.format(request)
                }.onFailure {
                    Log.w(TAG, "Formatter provider ${provider.id} failed", it)
                }.getOrNull()
            } ?: return null

        val replacementRange = result.replacementRange
        if (replacementRange == null) {
            text.replace(0, text.length, result.text)
        } else {
            text.replace(
                replacementRange.start.line,
                replacementRange.start.column,
                replacementRange.end.line,
                replacementRange.end.column,
                result.text
            )
        }
        return replacementRange
    }

    override fun formatRegionAsync(
        text: Content,
        range1: TextRange,
        range2: TextRange
    ): TextRange {
        formatAsync(text, range1)
        return range2
    }

    private companion object {
        const val TAG = "EditorFormatterRouter"
    }
}
