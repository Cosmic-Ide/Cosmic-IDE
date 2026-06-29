package org.cosmicide.ui.editor

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.material.color.MaterialColors
import io.github.rosemoe.sora.lang.EmptyLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import io.github.rosemoe.sora.lsp.client.connection.StreamConnectionProvider
import io.github.rosemoe.sora.lsp.client.languageserver.serverdefinition.CustomLanguageServerDefinition
import io.github.rosemoe.sora.lsp.editor.LspEditor
import io.github.rosemoe.sora.lsp.editor.LspProject
import io.github.rosemoe.sora.lsp.editor.text.MarkdownCodeHighlighterRegistry
import io.github.rosemoe.sora.lsp.editor.text.withEditorHighlighter
import io.github.rosemoe.sora.text.Content
import io.github.rosemoe.sora.widget.CodeEditor
import io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE
import io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_INNER
import io.github.rosemoe.sora.widget.CodeEditor.FLAG_DRAW_WHITESPACE_LEADING
import io.github.rosemoe.sora.widget.component.EditorDiagnosticTooltipWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.common.Prefs
import org.cosmicide.editor.language.KotlinLanguage
import org.cosmicide.editor.language.TsLanguageJava
import org.cosmicide.extension.setCompletionLayout
import org.cosmicide.extension.setFont
import org.cosmicide.util.ProjectHandler
import org.eclipse.lsp4j.CodeLensOptions
import org.eclipse.lsp4j.ServerCapabilities
import org.eclipse.lsp4j.SignatureHelpOptions
import org.eclipse.lsp4j.jsonrpc.messages.Either
import java.io.File
import java.io.InputStream
import java.io.OutputStream

data class CodeEditorState(
    var editor: CodeEditor? = null,
    val initialContent: Content = Content()
) {
    var content by mutableStateOf(initialContent)
}

fun setCodeEditorFactory(
    context: Context,
    state: CodeEditorState
): CodeEditor {
    val editor = CodeEditor(context)
    editor.apply {
        setText(state.content)
    }
    state.editor = editor
    return editor
}

@Composable
fun CodeEditor(
    modifier: Modifier = Modifier,
    state: CodeEditorState
) {
    val context = LocalContext.current
    val editor = remember {
        setCodeEditorFactory(
            context = context,
            state = state
        )
    }
    AndroidView(
        factory = { editor },
        modifier = modifier,
        onRelease = {
            it.release()
        }
    )
}

fun CodeEditor.applyEditorSettings(file: File) {
    colorScheme = TextMateColorScheme.create(ThemeRegistry.getInstance())
    setCompletionLayout()
    setTooltipImprovements()
    setFont()
    inputType = EditorInfo.TYPE_CLASS_TEXT or
            EditorInfo.TYPE_TEXT_FLAG_MULTI_LINE or
            EditorInfo.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
            EditorInfo.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD

    val flags = (FLAG_DRAW_WHITESPACE_LEADING
            or FLAG_DRAW_WHITESPACE_INNER
            or FLAG_DRAW_WHITESPACE_FOR_EMPTY_LINE)
    nonPrintablePaintingFlags = if (Prefs.nonPrintableCharacters) flags else 0

    setTextSize(Prefs.editorFontSize)
    tabWidth = Prefs.tabSize
    setInterceptParentHorizontalScrollIfNeeded(true)
    isLigatureEnabled = Prefs.useLigatures
    isWordwrap = Prefs.wordWrap
    setScrollBarEnabled(Prefs.scrollbarEnabled)
    isHardwareAcceleratedDrawAllowed = Prefs.hardwareAcceleration
    isLineNumberEnabled = Prefs.lineNumbers
    props.deleteEmptyLineFast = Prefs.quickDelete
    props.stickyScroll = Prefs.stickyScroll

    colorScheme = TextMateColorScheme.create(
        ThemeRegistry.getInstance().currentThemeModel
    )

    setEditorLanguage(file)
}

private fun CodeEditor.setEditorLanguage(file: File) {
    val project = ProjectHandler.getProject() ?: return

    when (file.extension) {
        "java" -> {
            if (Prefs.useJdtLS) {
                Toast.makeText(
                    this@setEditorLanguage.context,
                    "Connecting to JDT LS...",
                    Toast.LENGTH_SHORT
                ).show()
                this@setEditorLanguage.editable = false

                CoroutineScope(Dispatchers.IO).launch {
                    val jdtServerDefinition = CustomLanguageServerDefinition(
                        ext = "java",
                        serverConnectProvider = {
                            object : StreamConnectionProvider {
                                private var process: Process? = null
                                private var closed = false

                                override fun start() {
                                    process = jdtLspProcess

                                    closed = false
                                }

                                override val outputStream: OutputStream
                                    get() = process?.outputStream
                                        ?: throw IllegalStateException("Process not started")

                                override val inputStream: InputStream
                                    get() = process?.inputStream
                                        ?: throw IllegalStateException("Process not started")

                                override val isClosed: Boolean
                                    get() = closed

                                override fun close() {
                                    if (!closed) {
                                        process?.destroy() // Cleanly kill JDT LS when editor closes
                                        closed = true
                                    }
                                }
                            }
                        },
                        expectedCapabilitiesOverride = ServerCapabilities().apply {
                            codeActionProvider = Either.forLeft(true)
                            documentFormattingProvider = Either.forLeft(true)
                            signatureHelpProvider = SignatureHelpOptions(listOf("(", ","))
                            diagnosticProvider = null // DiagnosticRegistrationOptions(true, false)
                            definitionProvider = Either.forLeft(true)

                            hoverProvider =
                                Either.forLeft(true)               // Turn off hover AST generation
                            inlayHintProvider =
                                Either.forLeft(true)           // STOP compiling line parameters (Huge CPU save)
                            codeLensProvider =
                                CodeLensOptions(true)           // STOP rendering reference counters over definitions
                            semanticTokensProvider =
                                null                       // Let TextMate handle styling locally instead of LSP
                            documentHighlightProvider =
                                Either.forLeft(false)   // Stop cross-file variable color lookups
                        }
                    )

                    // Use the dynamic project root instead of a hardcoded path
                    val lspProject = LspProject(project.root.absolutePath)
                    lspProject.addServerDefinition(jdtServerDefinition)

                    val lspEditor: LspEditor = lspProject.createEditor(file.absolutePath)
                    val wrapperLanguage = createTextMateLanguage()

                    lspEditor.wrapperLanguage = wrapperLanguage
                    lspEditor.isEnableInlayHint = true
                    lspEditor.isEnableSignatureHelp = true

                    withContext(Dispatchers.Main) {
                        lspEditor.editor = this@setEditorLanguage
                    }

                    try {
                        lspEditor.connectWithTimeout()
                        this@setEditorLanguage.editable = true

                        lspEditor.requestManager.didChangeConfiguration(
                            org.eclipse.lsp4j.DidChangeConfigurationParams(
                                mapOf(
                                    "settings" to mapOf(
                                        "java" to mapOf(
                                            "autobuild" to mapOf("enabled" to false),
                                            "references" to mapOf("includeDecompiledSources" to false),
                                            "completion" to mapOf(
                                                "guessMethodArguments" to false,
                                                "favoriteStaticMembers" to emptyList<String>()
                                            ),
                                            "implementationsCodeLens" to mapOf("enabled" to false),
                                            "referencesCodeLens" to mapOf("enabled" to false)
                                        )
                                    )
                                )
                            )
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            } else {
                setEditorLanguage(
                    TsLanguageJava.getInstance(
                        this,
                        project,
                        file
                    )
                )
            }
        }

        "kt" -> {
            if (editorLanguage is KotlinLanguage) return
            setEditorLanguage(
                KotlinLanguage(
                    this,
                    project,
                    file
                )
            )
        }

        "class" -> {
            setEditorLanguage(
                TsLanguageJava.getInstance(
                    this,
                    project,
                    file
                )
            )
        }

        else -> {
            setEditorLanguage(EmptyLanguage())
        }
    }
}


private fun CodeEditor.setTooltipImprovements() {
    getComponent(EditorDiagnosticTooltipWindow::class.java).apply {
        setSize(500, 100)
        parentView.setBackgroundColor(
            MaterialColors.getColor(
                context,
                com.google.android.material.R.attr.colorSurface,
                null
            )
        )
    }
}

private fun createTextMateLanguage(): TextMateLanguage {
    MarkdownCodeHighlighterRegistry.global.withEditorHighlighter {
        Pair(TextMateLanguage.create("source.java", false), TextMateColorScheme.create(ThemeRegistry.getInstance()))
    }

    return TextMateLanguage.create(
        "source.java", false
    )
}
