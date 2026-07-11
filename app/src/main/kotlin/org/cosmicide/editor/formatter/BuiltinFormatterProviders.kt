package org.cosmicide.editor.formatter

import org.cosmicide.ide.editor.EditorExtensionPoints
import org.cosmicide.ide.editor.EditorFormatterProvider
import org.cosmicide.ide.editor.EditorFormatterRequest
import org.cosmicide.ide.editor.EditorFormatterResult
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.PluginIds

fun registerBuiltinFormatterExtensions(registry: MutableExtensionRegistry) {
    registry.register(
        point = EditorExtensionPoints.FORMATTER_PROVIDER,
        extension = KtfmtFormatterProvider,
        ownerPluginId = PluginIds.CORE,
        priority = KtfmtFormatterProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.FORMATTER_PROVIDER,
        extension = GoogleJavaFormatterProvider,
        ownerPluginId = PluginIds.CORE,
        priority = GoogleJavaFormatterProvider.priority
    )
}

private object KtfmtFormatterProvider : EditorFormatterProvider {
    override val id = "org.cosmicide.formatter.kotlin.ktfmt"
    override val priority = 100

    override fun supports(request: EditorFormatterRequest): Boolean {
        return request.file.extension == "kt" || request.file.extension == "kts"
    }

    override fun format(request: EditorFormatterRequest): EditorFormatterResult {
//        val formattingOptions = when (Prefs.ktfmtStyle) {
//            "google" -> Formatter.GOOGLE_FORMAT
//            "meta" -> Formatter.META_FORMAT
//            else -> Formatter.KOTLINLANG_FORMAT
//        }
        return EditorFormatterResult(
            text = request.text // Formatter.format(formattingOptions, request.text)
        )
    }
}

private object GoogleJavaFormatterProvider : EditorFormatterProvider {
    override val id = "org.cosmicide.formatter.java.google"
    override val priority = 100

    override fun supports(request: EditorFormatterRequest): Boolean {
        return request.file.extension == "java"
    }

    override fun format(request: EditorFormatterRequest): EditorFormatterResult {
        return EditorFormatterResult(
            text = request.text //GoogleJavaFormat.formatCode(request.text)
        )
    }
}
