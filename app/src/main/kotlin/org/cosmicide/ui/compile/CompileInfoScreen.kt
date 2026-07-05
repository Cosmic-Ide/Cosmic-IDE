package org.cosmicide.ui.compile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cosmicide.build.BuildReporter
import org.cosmicide.compile.Compiler
import org.cosmicide.extension.setFont
import org.cosmicide.ui.editor.CodeEditor
import org.cosmicide.ui.editor.CodeEditorState
import org.cosmicide.util.ProjectHandler

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CompileInfoScreen(
    onNavigateBack: () -> Unit,
    onCompileSuccess: () -> Unit
) {
    val project = remember {
        ProjectHandler.getProject() ?: throw IllegalStateException("No project set")
    }

    var consoleLogs by rememberSaveable { mutableStateOf("") }
    var hasCompiled by rememberSaveable { mutableStateOf(false) }

    val editorState = remember(consoleLogs) {
        CodeEditorState(initialContent = io.github.rosemoe.sora.text.Content(consoleLogs))
    }

    LaunchedEffect(Unit) {
        editorState.editor?.apply {
            setEditorLanguage(TextMateLanguage.create("source.build", false))
            editable = false
            isLineNumberEnabled = false
            colorScheme = TextMateColorScheme.create(
                ThemeRegistry.getInstance().currentThemeModel
            )
            setFont()
        }

        if (hasCompiled) return@LaunchedEffect

        withContext(Dispatchers.IO) {
            val reporter = BuildReporter { report ->
                if (report.message.isEmpty()) return@BuildReporter

                val logLine = "${report.kind}: ${report.message}\n"
                consoleLogs += logLine

                editorState.editor?.post {
                    val buffer = editorState.editor?.text ?: return@post
                    buffer.insert(buffer.lineCount - 1, 0, logLine)
                }
            }

            val compiler = Compiler(project, reporter)

            try {
                compiler.compile()

                hasCompiled = true

                if (reporter.buildSuccess) {
                    withContext(Dispatchers.Main) {
                        onCompileSuccess()
                    }
                }
            } catch (e: Exception) {
                editorState.editor?.post {
                    val errorBuffer = editorState.editor?.text ?: return@post
                    errorBuffer.insert(
                        errorBuffer.lineCount - 1,
                        0,
                        "Build failed: ${e.message}\n"
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "Compiling ${project.name}",
                            style = MaterialTheme.typography.titleMediumEmphasized,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
                HorizontalDivider(thickness = 1.dp)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            CodeEditor(
                modifier = Modifier.fillMaxSize(),
                state = editorState
            )
        }
    }
}