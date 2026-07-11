package org.cosmicide.editor.language

import io.github.rosemoe.sora.lang.EmptyLanguage
import org.cosmicide.editor.formatter.registerBuiltinFormatterExtensions
import org.cosmicide.editor.lsp.JdtLspServerProvider
import org.cosmicide.editor.lsp.KotlinLspServerProvider
import org.cosmicide.editor.lsp.LspEditorLanguageProvider
import org.cosmicide.editor.lsp.MetalsLspServerProvider
import org.cosmicide.ide.editor.EditorExtensionPoints
import org.cosmicide.ide.editor.EditorLanguageProvider
import org.cosmicide.ide.editor.EditorLanguageRequest
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.PluginIds

fun registerBuiltinEditorExtensions(registry: MutableExtensionRegistry) {
    registerBuiltinFormatterExtensions(registry)

    registry.register(
        point = EditorExtensionPoints.LANGUAGE_PROVIDER,
        extension = JavaEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = JavaEditorLanguageProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LANGUAGE_PROVIDER,
        extension = LspEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = LspEditorLanguageProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
        extension = JdtLspServerProvider,
        ownerPluginId = PluginIds.CORE,
        priority = JdtLspServerProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LANGUAGE_PROVIDER,
        extension = KotlinEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = KotlinEditorLanguageProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
        extension = KotlinLspServerProvider,
        ownerPluginId = PluginIds.CORE,
        priority = KotlinLspServerProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LANGUAGE_PROVIDER,
        extension = ScalaEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = ScalaEditorLanguageProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
        extension = MetalsLspServerProvider,
        ownerPluginId = PluginIds.CORE,
        priority = MetalsLspServerProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LANGUAGE_PROVIDER,
        extension = EmptyEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = EmptyEditorLanguageProvider.priority
    )
}

private object EmptyEditorLanguageProvider : EditorLanguageProvider {
    override val id = "org.cosmicide.editor.empty"
    override val priority = Int.MIN_VALUE

    override fun supports(request: EditorLanguageRequest): Boolean {
        return true
    }

    override fun configure(request: EditorLanguageRequest): Boolean {
        request.editor.setEditorLanguage(EmptyLanguage())
        return true
    }
}
