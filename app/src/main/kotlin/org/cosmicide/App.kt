/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide

import android.app.Application
import android.os.Build
import io.github.rosemoe.sora.langs.textmate.registry.FileProviderRegistry
import io.github.rosemoe.sora.langs.textmate.registry.GrammarRegistry
import io.github.rosemoe.sora.langs.textmate.registry.provider.AssetsFileResolver
import io.github.rosemoe.sora.lsp.client.languageserver.wrapper.LanguageServerWrapper
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.events.EventContext
import io.github.rosemoe.sora.lsp.events.EventType
import io.github.rosemoe.sora.lsp.events.document.applyEdits
import io.github.rosemoe.sora.lsp.events.getByClass
import io.github.rosemoe.sora.lsp.events.workspace.WorkSpaceApplyEditEvent
import io.github.rosemoe.sora.lsp.utils.LSPException
import io.github.rosemoe.sora.lsp.utils.toFileUri
import io.github.rosemoe.sora.lsp.utils.toURI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.cosmicide.common.Analytics
import org.cosmicide.common.IndexManager
import org.cosmicide.common.MemoryMonitor
import org.cosmicide.common.MemoryUtils
import org.cosmicide.common.Prefs
import org.cosmicide.editor.lsp.handleLspShowDocument
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.plugin.runtime.hook.Hook
import org.cosmicide.plugin.runtime.hook.HookManager
import org.cosmicide.util.FileUtil
import org.eclipse.lsp4j.ApplyWorkspaceEditParams
import org.eclipse.lsp4j.ClientCapabilities
import org.eclipse.lsp4j.CodeActionCapabilities
import org.eclipse.lsp4j.CodeActionKind
import org.eclipse.lsp4j.CodeActionKindCapabilities
import org.eclipse.lsp4j.CodeActionLiteralSupportCapabilities
import org.eclipse.lsp4j.ColorProviderCapabilities
import org.eclipse.lsp4j.CompletionCapabilities
import org.eclipse.lsp4j.CompletionItemCapabilities
import org.eclipse.lsp4j.DefinitionCapabilities
import org.eclipse.lsp4j.DiagnosticCapabilities
import org.eclipse.lsp4j.DidChangeWatchedFilesCapabilities
import org.eclipse.lsp4j.DocumentHighlightCapabilities
import org.eclipse.lsp4j.ExecuteCommandCapabilities
import org.eclipse.lsp4j.FormattingCapabilities
import org.eclipse.lsp4j.HoverCapabilities
import org.eclipse.lsp4j.InitializeParams
import org.eclipse.lsp4j.InlayHintCapabilities
import org.eclipse.lsp4j.MarkupKind
import org.eclipse.lsp4j.OnTypeFormattingCapabilities
import org.eclipse.lsp4j.PublishDiagnosticsCapabilities
import org.eclipse.lsp4j.RangeFormattingCapabilities
import org.eclipse.lsp4j.ReferencesCapabilities
import org.eclipse.lsp4j.RenameCapabilities
import org.eclipse.lsp4j.ShowDocumentCapabilities
import org.eclipse.lsp4j.ShowDocumentParams
import org.eclipse.lsp4j.SignatureHelpCapabilities
import org.eclipse.lsp4j.SignatureInformationCapabilities
import org.eclipse.lsp4j.SymbolCapabilities
import org.eclipse.lsp4j.SynchronizationCapabilities
import org.eclipse.lsp4j.TextDocumentClientCapabilities
import org.eclipse.lsp4j.TextEdit
import org.eclipse.lsp4j.WindowClientCapabilities
import org.eclipse.lsp4j.WorkspaceClientCapabilities
import org.eclipse.lsp4j.WorkspaceEdit
import org.eclipse.lsp4j.WorkspaceEditCapabilities
import org.eclipse.lsp4j.WorkspaceFolder
import org.eclipse.lsp4j.jsonrpc.services.GenericEndpoint
import org.lsposed.hiddenapibypass.HiddenApiBypass
import top.canyie.pine.Pine
import java.io.File
import java.lang.ref.WeakReference
import java.net.URI
import java.time.ZonedDateTime
import java.util.Locale
import java.util.TimeZone

class App : Application() {

    private lateinit var memoryMonitor: MemoryMonitor

    companion object {

        /**
         * The application instance.
         */
        @JvmStatic
        lateinit var instance: WeakReference<App>
    }

    override fun onCreate() {
        super.onCreate()

        if (FileUtil.isInitialized.not()) return

        Analytics.init(this@App)

        Analytics.logEvent(
            "user_metrics",
            "name" to Prefs.clientName,
            "theme" to Prefs.appTheme,
            "language" to Locale.getDefault().language,
            "timezone" to TimeZone.getDefault().id,
            "sdk" to Build.VERSION.SDK_INT.toString() + " (" + Build.SUPPORTED_ABIS.joinToString(", ") + ")",
            "device" to Build.DEVICE + " " + Build.DEVICE + " " + Build.PRODUCT,
            "fingerprint" to Build.FINGERPRINT,
            "hardware" to Build.HARDWARE,
            "version" to BuildConfig.VERSION_NAME + if (BuildConfig.GIT_COMMIT.isNotEmpty()) " (${BuildConfig.GIT_COMMIT})" else "",
        )
        Analytics.logEvent(
            "app_start",
            "time" to ZonedDateTime.now().toString(),
        )

        instance = WeakReference(this)
        HookManager.context = WeakReference(this)

        IndexManager.init(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions()
        }

        loadTextmateTheme()

        CosmicPluginHost.init(this)

        memoryMonitor = MemoryMonitor(intervalMs = 5000L) { pressure ->
            if (pressure == MemoryUtils.MemoryPressure.CRITICAL) {
                android.util.Log.w("App", "Critical memory pressure: throttling LSP servers")
            }
        }
        memoryMonitor.start()

        Analytics.setAnalyticsCollectionEnabled(Prefs.analyticsEnabled)
        applyLSP4JHooks()

    }

    override fun onTerminate() {
        super.onTerminate()
        if (::memoryMonitor.isInitialized) {
            memoryMonitor.stop()
        }
        IndexManager.shutdown()
    }

    fun loadTextmateTheme() {
        val fileProvider = AssetsFileResolver(assets)
        FileProviderRegistry.getInstance().addFileProvider(fileProvider)

        GrammarRegistry.getInstance().loadGrammars("textmate/languages.json")
    }

    private fun applyLSP4JHooks() {
        HookManager.registerHook(object : Hook(
            method = "getInitParams",
            argTypes = arrayOf(),
            type = LanguageServerWrapper::class.java
        ) {
            override fun before(param: Pine.CallFrame) {
                val wrapper = param.thisObject as? LanguageServerWrapper ?: return

                val initParams = InitializeParams().apply {
                    rootUri = wrapper.project.projectUri.toUri().toASCIIString()
                }

                val workspaceClientCapabilities = WorkspaceClientCapabilities().apply {
                    applyEdit = true
                    didChangeWatchedFiles = DidChangeWatchedFilesCapabilities()
                    executeCommand = ExecuteCommandCapabilities()
                    workspaceEdit = WorkspaceEditCapabilities()
                    symbol = SymbolCapabilities()
                    workspaceFolders = true
                    configuration = false
                }
                val workspaceFolder = WorkspaceFolder().apply {
                    uri = initParams.rootUri
                    name = File(URI.create(uri)).name
                }
                // Maybe the user should be allowed to customize the WorkspaceFolder?
                // workspaceFolder.setName("")
                initParams.workspaceFolders = listOf(workspaceFolder)

                val markupKinds = listOf(
                    MarkupKind.PLAINTEXT, MarkupKind.MARKDOWN
                )
                val textDocumentClientCapabilities = TextDocumentClientCapabilities().apply {
                    codeAction = CodeActionCapabilities()
                    codeAction.codeActionLiteralSupport =
                        CodeActionLiteralSupportCapabilities().apply {
                            codeAction = CodeActionCapabilities().apply {
                                codeActionLiteralSupport =
                                    CodeActionLiteralSupportCapabilities().apply {
                                        codeActionKind = CodeActionKindCapabilities(
                                            listOf(
                                                CodeActionKind.Empty,
                                                CodeActionKind.QuickFix,
                                                CodeActionKind.Refactor,
                                                CodeActionKind.RefactorExtract,
                                                CodeActionKind.RefactorInline,
                                                CodeActionKind.RefactorRewrite,
                                                CodeActionKind.Source,
                                                CodeActionKind.SourceOrganizeImports,
                                                CodeActionKind.Notebook,
                                                CodeActionKind.RefactorMove,
                                                CodeActionKind.SourceFixAll
                                            )
                                        )
                                    }
                            }
                        }
                    completion =
                        CompletionCapabilities(
                            CompletionItemCapabilities(true).apply {
                                deprecatedSupport = true
                                labelDetailsSupport = true
                                commitCharactersSupport = true
                                insertReplaceSupport = true
                                preselectSupport = true
                            }
                        )
                    definition = DefinitionCapabilities(true, true)
                    documentHighlight = DocumentHighlightCapabilities()
                    colorProvider = ColorProviderCapabilities()
                    inlayHint = InlayHintCapabilities()
                    formatting = FormattingCapabilities()
                    hover = HoverCapabilities(markupKinds, true)
                    onTypeFormatting = OnTypeFormattingCapabilities()
                    rangeFormatting = RangeFormattingCapabilities()
                    references = ReferencesCapabilities()
                    rename = RenameCapabilities(true, true)
                    signatureHelp =
                        SignatureHelpCapabilities(
                            SignatureInformationCapabilities(markupKinds),
                            true
                        )
                    synchronization =
                        SynchronizationCapabilities(true, true, true)
                    publishDiagnostics = PublishDiagnosticsCapabilities(true)
                    diagnostic =
                        DiagnosticCapabilities(true, true).apply { relatedInformation = true }
                }
                val windowClientCapabilities = WindowClientCapabilities().apply {
                    showDocument = ShowDocumentCapabilities(true)
                }

                initParams.apply {
                    capabilities =
                        ClientCapabilities(
                            workspaceClientCapabilities,
                            textDocumentClientCapabilities,
                            windowClientCapabilities
                        )
                    initializationOptions =
                        wrapper.serverDefinition.getInitializationOptions(URI.create(initParams.rootUri))
                }
                param.result = initParams
                super.after(param)
            }
        })

        HookManager.registerHook(object : Hook(
            method = "request",
            argTypes = arrayOf(String::class.java, Any::class.java),
            type = GenericEndpoint::class.java
        ) {
            override fun before(param: Pine.CallFrame) {
                if (param.args.first() != "window/showDocument") return
                val request = param.args.last() as? ShowDocumentParams ?: return
                param.result = handleLspShowDocument(request)
            }
        })

        HookManager.registerHook(object : Hook(
            method = "handle",
            argTypes = arrayOf(EventContext::class.java),
            type = WorkSpaceApplyEditEvent::class.java
        ) {
            override fun before(param: Pine.CallFrame) {
                val context = param.args.first() as EventContext
                if (context.getByClass<ApplyWorkspaceEditParams>() != null) return

                val edit = context.getByClass<WorkspaceEdit>() ?: return
                context.put(ApplyWorkspaceEditParams(edit))
            }
        })

        HookManager.registerHook(object : Hook(
            method = "applyChanges",
            argTypes = arrayOf(
                EventContext::class.java,
                Map::class.java
            ),
            type = WorkSpaceApplyEditEvent::class.java
        ) {
            override fun before(param: Pine.CallFrame) {
                val context = param.args.first() as EventContext
                val changes = param.args.last() as? Map<*, *> ?: return

                val project = context.getByClass<LspProject>() ?: return

                runBlocking {
                    withContext(Dispatchers.Main.immediate) {
                        changes.forEach { (rawUri, rawTextEdits) ->
                            val uri = rawUri as? String ?: return@forEach
                            val textEdits = (rawTextEdits as? List<*>)
                                ?.filterIsInstance<TextEdit>()
                                ?: return@forEach
                            val fileUri = uri.toURI().toFileUri()

                            val editor = project.getEditor(fileUri)
                                ?: throw LSPException("The url $uri is not opened.")

                            val codeEditor = editor.editor
                                ?: throw LSPException("The editor content $uri is null.")
                            val cursorAnimationEnabled = codeEditor.isCursorAnimationEnabled
                            codeEditor.isCursorAnimationEnabled = false
                            try {
                                editor.eventManager.emit(EventType.applyEdits) {
                                    put("edits", textEdits)
                                    put("content", codeEditor.text)
                                }
                            } finally {
                                codeEditor.isCursorAnimationEnabled = cursorAnimationEnabled
                            }
                        }
                    }
                }

                // Sora looks up editors with the raw URI here instead of its normalized FileUri.
                param.result = null
            }
        })
    }
}
