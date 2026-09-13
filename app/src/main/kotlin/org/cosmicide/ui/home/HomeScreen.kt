package org.cosmicide.ui.home

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.cosmicide.R
import org.cosmicide.app.LocalAppContainer
import org.cosmicide.common.Analytics
import org.cosmicide.model.ProjectViewModel
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectCreationProvider
import org.cosmicide.ui.donation.DonationPromptTracker
import org.cosmicide.ui.donation.DonationSheet
import org.cosmicide.ui.plugin.ProjectActionDialog
import org.cosmicide.ui.plugin.ProjectCreationDialog
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToSettings: () -> Unit,
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
    var projectForIconPick by remember { mutableStateOf<Project?>(null) }
    var iconUpdateTrigger by remember { mutableIntStateOf(0) }
    var showCreationProviders by remember { mutableStateOf(false) }
    var selectedCreationProvider by remember { mutableStateOf<ProjectCreationProvider?>(null) }
    var selectedProjectAction by remember { mutableStateOf<ProjectActionContribution?>(null) }
    val creationProviders = remember(extensionRepository) {
        extensionRepository.creationProviders()
    }
    val actionProviders = remember(extensionRepository) {
        extensionRepository.actionProviders()
    }

    val listState = rememberLazyListState()

    val creationProviderSheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.PartiallyExpanded, SheetValue.Expanded),
    )
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val iconPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent(),
    ) { uri ->
        val project = projectForIconPick
        if ((uri != null) && (project != null)) {
            scope.launch(Dispatchers.IO) {
                runCatching {
                    val cosmicDir = File(project.root, ".cosmic").apply { mkdirs() }
                    val iconFile = File(cosmicDir, "icon.png")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        iconFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }
                }.onSuccess {
                    iconUpdateTrigger++
                    scope.launch { snackbarHostState.showSnackbar("Project icon updated") }
                }.onFailure {
                    scope.launch { snackbarHostState.showSnackbar("Failed to update project icon") }
                }
                projectForIconPick = null
            }
        }
    }

    val onChangeIcon: (Project) -> Unit = { project ->
        projectForIconPick = project
        iconPickerLauncher.launch("image/*")
    }

    val onResetIcon: (Project) -> Unit = { project ->
        val iconFile = File(project.root, ".cosmic/icon.png")
        if (iconFile.exists()) {
            iconFile.delete()
            iconUpdateTrigger++
            scope.launch { snackbarHostState.showSnackbar("Project icon reset") }
        }
    }

    val openProjectCreation: () -> Unit = {
        if (creationProviders.isEmpty()) {
            scope.launch {
                snackbarHostState.showSnackbar(
                    "No installed plugin provides project creation"
                )
            }
        } else {
            showCreationProviders = true
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
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.projects),
                        style = MaterialTheme.typography.headlineMediumEmphasized,
                    )
                },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(
                        onClick = onNavigateToSettings,
                        shapes = IconButtonDefaults.shapes(),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = openProjectCreation,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("New Project") },
                expanded = !listState.canScrollBackward,
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            if (projects.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(contentPadding),
                ) {
                    EmptyProjectsState(
                        onCreateClick = openProjectCreation
                    )
                }
            } else {
                PullToRefreshBox(
                    isRefreshing = isLoading,
                    onRefresh = { viewModel.loadProjects() },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            end = 16.dp,
                            top = contentPadding.calculateTopPadding() + 8.dp,
                            bottom = contentPadding.calculateBottomPadding() + 96.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        itemsIndexed(
                            projects,
                            key = { _, project -> project.root.absolutePath }) { index, project ->
                            ProjectCard(
                                project = project,
                                index = index,
                                totalCount = projects.size,
                                pluginActions = actionProviders.flatMap { provider ->
                                    provider.actions(project).map { action ->
                                        ProjectActionContribution(provider, action, project)
                                    }
                                },
                                onPluginAction = { selectedProjectAction = it },
                                onChangeIcon = { onChangeIcon(project) },
                                onResetIcon = { onResetIcon(project) },
                                iconUpdateTrigger = iconUpdateTrigger,
                                onClick = { onNavigateToEditor(project) },
                                onDelete = { projectToDelete = project },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreationProviders) {
        ModalBottomSheet(
            onDismissRequest = { showCreationProviders = false },
            sheetState = creationProviderSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    text = "Create project",
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = "Choose an installed plugin",
                    modifier = Modifier.padding(horizontal = 24.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(creationProviders) { index, provider ->
                        SegmentedListItem(
                            onClick = {
                                showCreationProviders = false
                                selectedCreationProvider = provider
                            },
                            shapes = ListItemDefaults.segmentedShapes(
                                index,
                                creationProviders.size
                            ),
                            colors = ListItemDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            supportingContent = {
                                if (provider.description.isNotBlank()) {
                                    Text(provider.description)
                                }
                            }
                        ) {
                            Text(provider.displayName)
                        }
                    }
                }
            }
        }
    }

    if (showAnalyticsDialog) {
        AnalyticsDialog(onDismiss = { showAnalyticsDialog = false }, onAccept = {
            prefs.edit {
                putBoolean("analytics_preference", true)
                putBoolean("analytics_preference_asked", true)
            }
            showAnalyticsDialog = false
        }, onDecline = {
            prefs.edit {
                putBoolean("analytics_preference", false)
                putBoolean("analytics_preference_asked", true)
            }
            Analytics.setAnalyticsCollectionEnabled(false)
            showAnalyticsDialog = false
        })
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
            })
    }

    selectedCreationProvider?.let { provider ->
        ProjectCreationDialog(
            provider = provider,
            projectsDirectory = archiveRepository.projectsDirectory,
            onDismiss = { selectedCreationProvider = null },
            onProjectCreated = { project, message ->
                selectedCreationProvider = null
                viewModel.loadProjects()
                scope.launch { snackbarHostState.showSnackbar(message) }
                onNavigateToEditor(project)
            })
    }

    selectedProjectAction?.let { contribution ->
        ProjectActionDialog(
            provider = contribution.provider,
            action = contribution.action,
            project = contribution.project,
            onDismiss = { selectedProjectAction = null },
            onCompleted = { message, refreshProject ->
                if (refreshProject) viewModel.loadProjects()
                scope.launch { snackbarHostState.showSnackbar(message) }
            })
    }
}
