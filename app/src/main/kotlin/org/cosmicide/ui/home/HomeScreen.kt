package org.cosmicide.ui.home

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import org.cosmicide.R
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.common.Analytics
import org.cosmicide.model.ProjectViewModel
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCreationProvider
import org.cosmicide.project.TerminalAction
import org.cosmicide.ui.donation.DonationPromptTracker
import org.cosmicide.ui.donation.DonationSheet
import org.cosmicide.ui.plugin.ProjectActionDialog
import org.cosmicide.ui.plugin.ProjectCreationDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToNewProject: () -> Unit,
    onNavigateToTerminal: (TerminalAction, File) -> Unit,
    onNavigateToEditor: (Project) -> Unit
) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val viewModel: ProjectViewModel = viewModel(factory = container.projectViewModelFactory)
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val archiveRepository = container.homeProjectArchiveRepository
    val extensionRepository = container.homeExtensionRepository

    var projectToDelete by remember { mutableStateOf<Project?>(null) }
    var projectToBackup by remember { mutableStateOf<Project?>(null) }
    var showCreationMenu by remember { mutableStateOf(false) }
    var selectedCreationProvider by remember { mutableStateOf<ProjectCreationProvider?>(null) }
    var selectedProjectAction by remember { mutableStateOf<ProjectActionContribution?>(null) }
    val creationProviders = remember { extensionRepository.creationProviders() }
    val actionProviders = remember { extensionRepository.actionProviders() }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val backupLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val project = projectToBackup
        if (uri != null && project != null) {
            scope.launch {
                runCatching { archiveRepository.backup(project, uri) }
                    .onSuccess {
                        snackbarHostState.showSnackbar("Project backed up successfully")
                    }
                    .onFailure { error ->
                        snackbarHostState.showSnackbar(
                            error.message ?: "Project backup failed"
                        )
                    }
                projectToBackup = null
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                runCatching { archiveRepository.importArchive(uri) }
                    .onSuccess {
                        viewModel.loadProjects()
                        snackbarHostState.showSnackbar("Project imported successfully")
                    }
                    .onFailure { error ->
                        snackbarHostState.showSnackbar(
                            error.message ?: "Project import failed"
                        )
                    }
            }
        }
    }

    var showAnalyticsDialog by remember { mutableStateOf(false) }
    var showDonationSheet by remember { mutableStateOf(false) }
    val prefs =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    LaunchedEffect(Unit) {
        if (!prefs.getBoolean("analytics_preference_asked", false)) {
            showAnalyticsDialog = true
        }
    }

    LaunchedEffect(projects.size, isLoading) {
        val analyticsChoiceMade = prefs.getBoolean("analytics_preference_asked", false)
        if (!isLoading && analyticsChoiceMade) {
            showDonationSheet = DonationPromptTracker.claimPrompt(context, projects.size)
        }
    }

    Scaffold(
        modifier = Modifier
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .background(MaterialTheme.colorScheme.surfaceContainer),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.projects),
                        style = MaterialTheme.typography.headlineMediumEmphasized
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        }
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(contentPadding)
        ) {
            if (projects.isEmpty() && !isLoading) {
                EmptyProjectsState(onCreateClick = onNavigateToNewProject)
            } else {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.loadProjects() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 24.dp,
                            end = 24.dp,
                            top = 16.dp,
                            bottom = 112.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        items(projects, key = { it.root.absolutePath }) { project ->
                            ProjectCard(
                                project = project,
                                pluginActions = actionProviders.flatMap { provider ->
                                    provider.actions(project).map { action ->
                                        ProjectActionContribution(provider, action, project)
                                    }
                                },
                                onPluginAction = { selectedProjectAction = it },
                                onClick = { onNavigateToEditor(project) },
                                onBackup = {
                                    projectToBackup = project
                                    backupLauncher.launch("${project.name}.zip")
                                },
                                onDelete = { projectToDelete = project }
                            )
                        }
                    }
                }
            }

            HorizontalFloatingToolbar(
                expanded = true,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
                floatingActionButton = {
                    FloatingToolbarDefaults.StandardFloatingActionButton(
                        onClick = onNavigateToNewProject,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Project"
                        )
                    }
                }
            ) {
                if (creationProviders.isNotEmpty()) {
                    Box {
                        IconButton(onClick = { showCreationMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Create or import with plugin",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        DropdownMenu(
                            expanded = showCreationMenu,
                            onDismissRequest = { showCreationMenu = false }
                        ) {
                            creationProviders.forEach { provider ->
                                DropdownMenuItem(
                                    text = { Text(provider.displayName) },
                                    onClick = {
                                        showCreationMenu = false
                                        selectedCreationProvider = provider
                                    }
                                )
                            }
                        }
                    }
                }
                IconButton(onClick = { importLauncher.launch(arrayOf("application/zip")) }) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = "Import ZIP",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showAnalyticsDialog) {
        AnalyticsDialog(
            onDismiss = { showAnalyticsDialog = false },
            onAccept = {
                prefs.edit {
                    putBoolean("analytics_preference", true)
                    putBoolean("analytics_preference_asked", true)
                }
                showAnalyticsDialog = false
            },
            onDecline = {
                prefs.edit {
                    putBoolean("analytics_preference", false)
                    putBoolean("analytics_preference_asked", true)
                }
                Analytics.setAnalyticsCollectionEnabled(false)
                showAnalyticsDialog = false
            }
        )
    }

    if (showDonationSheet) {
        DonationSheet(onDismiss = { showDonationSheet = false })
    }

    if (projectToDelete != null) {
        DeleteProjectDialog(
            project = projectToDelete!!,
            onDismiss = { projectToDelete = null },
            onConfirm = {
                viewModel.deleteProject(projectToDelete!!)
                projectToDelete = null
            }
        )
    }

    selectedCreationProvider?.let { provider ->
        ProjectCreationDialog(
            provider = provider,
            projectsDirectory = archiveRepository.projectsDirectory,
            onDismiss = { selectedCreationProvider = null },
            onRunInTerminal = { action ->
                selectedCreationProvider = null
                onNavigateToTerminal(action, archiveRepository.projectsDirectory)
            },
            onProjectCreated = { project, message ->
                selectedCreationProvider = null
                viewModel.loadProjects()
                scope.launch { snackbarHostState.showSnackbar(message) }
                onNavigateToEditor(project)
            }
        )
    }

    selectedProjectAction?.let { contribution ->
        ProjectActionDialog(
            provider = contribution.provider,
            action = contribution.action,
            project = contribution.project,
            onDismiss = { selectedProjectAction = null },
            onRunInTerminal = { action ->
                selectedProjectAction = null
                onNavigateToTerminal(action, contribution.project.root)
            },
            onCompleted = { message, refreshProject ->
                if (refreshProject) viewModel.loadProjects()
                scope.launch { snackbarHostState.showSnackbar(message) }
            }
        )
    }
}
