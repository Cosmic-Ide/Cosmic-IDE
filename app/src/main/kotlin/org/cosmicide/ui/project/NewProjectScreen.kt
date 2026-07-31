package org.cosmicide.ui.project

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.cosmicide.R
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.model.ProjectViewModel
import org.cosmicide.project.Project

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewProjectScreen(
    onBack: () -> Unit,
    onNavigateToEditor: (Project) -> Unit
) {
    val container = LocalAppContainer.current
    val viewModel: ProjectViewModel = viewModel(factory = container.projectViewModelFactory)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val formScrollState = rememberScrollState()
    val creationLogScrollState = rememberScrollState()
    val projectsDirectory = container.projectsDirectory

    var formState by remember { mutableStateOf(NewProjectFormState()) }
    var isCreating by remember { mutableStateOf(false) }
    var creationLog by remember { mutableStateOf("") }
    val validation = validateNewProjectForm(formState, projectsDirectory)

    fun appendCreationLog(text: String) {
        if (text.isEmpty()) return
        scope.launch {
            creationLog = (creationLog + text).takeLast(MAX_VISIBLE_LOG_CHARS)
        }
    }

    fun createProject() {
        if (!validation.isValid || isCreating) return

        isCreating = true
        scope.launch {

        }
    }

    LaunchedEffect(creationLog) {
        creationLogScrollState.scrollTo(creationLogScrollState.maxValue)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.new_project),
                        style = MaterialTheme.typography.headlineMediumEmphasized
                    )
                },
                scrollBehavior = scrollBehavior,
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !isCreating) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { contentPadding ->
        NewProjectFormContent(
            state = formState,
            validation = validation,
            isCreating = isCreating,
            creationLog = creationLog,
            creationLogScrollState = creationLogScrollState,
            onStateChange = { formState = it },
            onCreate = ::createProject,
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(formScrollState)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        )
    }
}

private const val MAX_VISIBLE_LOG_CHARS = 24_000
