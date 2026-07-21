package org.cosmicide.editor.language

import android.content.Context
import io.github.rosemoe.sora.lang.EmptyLanguage
import org.cosmicide.editor.formatter.registerBuiltinFormatterExtensions
import org.cosmicide.editor.lsp.CustomLspConfigurationStore
import org.cosmicide.editor.lsp.CustomLspServerProvider
import org.cosmicide.editor.lsp.LspEditorLanguageProvider
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorLanguageProvider
import org.cosmicide.editor.EditorLanguageRequest
import org.cosmicide.plugin.api.MutableExtensionRegistry
import org.cosmicide.plugin.api.PluginIds

fun registerBuiltinEditorExtensions(context: Context, registry: MutableExtensionRegistry) {
    registerBuiltinFormatterExtensions(registry)

    registry.register(
        point = EditorExtensionPoints.LANGUAGE_PROVIDER,
        extension = LspEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = LspEditorLanguageProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
        extension = JavaEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = JavaEditorLanguageProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
        extension = KotlinEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = KotlinEditorLanguageProvider.priority
    )
    registry.register(
        point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
        extension = ScalaEditorLanguageProvider,
        ownerPluginId = PluginIds.CORE,
        priority = ScalaEditorLanguageProvider.priority
    )
    val customLspProvider = CustomLspServerProvider(
        context.applicationContext,
        CustomLspConfigurationStore(context)
    )
    registry.register(
        point = EditorExtensionPoints.LSP_SERVER_PROVIDER,
        extension = customLspProvider,
        ownerPluginId = PluginIds.CORE,
        priority = customLspProvider.priority
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
    override val canDisable = false

    override fun supports(request: EditorLanguageRequest): Boolean {
        return true
    }

    override fun configure(request: EditorLanguageRequest): Boolean {
        request.editor.setEditorLanguage(EmptyLanguage())
        return true
    }
}
