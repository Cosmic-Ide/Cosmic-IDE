/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.git.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.plugin.git.GitBranch
import org.cosmicide.plugin.git.GitCommit
import org.cosmicide.plugin.git.GitRemote
import org.cosmicide.plugin.git.GitService
import org.cosmicide.plugin.git.GitStatus
import org.cosmicide.plugin.git.GitUserConfig
import org.cosmicide.plugin.git.ui.tabs.GitBranchesTab
import org.cosmicide.plugin.git.ui.tabs.GitChangesTab
import org.cosmicide.plugin.git.ui.tabs.GitHistoryTab
import org.cosmicide.plugin.git.ui.tabs.GitSettingsTab
import org.cosmicide.ui.plugin.LocalPluginBackHandler
import org.cosmicide.util.FileUtil
import java.io.File

enum class GitScreenTab(val label: String) {
    CHANGES("Changes"),
    BRANCHES("Branches"),
    HISTORY("History"),
    SETTINGS("Settings")
}

@Composable
fun GitScreen(
    gitService: GitService,
    args: Map<String, String> = emptyMap(),
    onBack: (() -> Unit)? = null
) {
    val navBack = onBack ?: LocalPluginBackHandler.current
    if (navBack != null) {
        BackHandler(onBack = navBack)
    }

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val initialProjectPath = args["project_path"]
    val initialTabName = args["initial_tab"]?.uppercase()

    val initialTab = remember(initialTabName) {
        GitScreenTab.entries.firstOrNull { it.name == initialTabName }
            ?: if (initialProjectPath != null) GitScreenTab.CHANGES else GitScreenTab.SETTINGS
    }
    var selectedTab by remember { mutableStateOf(initialTab) }

    var availableProjects by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedProjectDir by remember {
        mutableStateOf(initialProjectPath?.let { File(it) })
    }
    var isProjectDropdownExpanded by remember { mutableStateOf(false) }

    var isRepository by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf(GitStatus()) }
    var branches by remember { mutableStateOf<List<GitBranch>>(emptyList()) }
    var remotes by remember { mutableStateOf<List<GitRemote>>(emptyList()) }
    var commits by remember { mutableStateOf<List<GitCommit>>(emptyList()) }
    var globalConfig by remember { mutableStateOf(GitUserConfig()) }
    var localConfig by remember { mutableStateOf(GitUserConfig()) }
    var gitVersion by remember { mutableStateOf("Checking…") }
    var operating by remember { mutableStateOf(false) }

    fun refreshRepoData(projectDir: File?) {
        scope.launch {
            operating = true
            try {
                gitVersion = gitService.getGitVersion(projectDir ?: FileUtil.projectDir)
                globalConfig =
                    gitService.getUserConfig(projectDir ?: FileUtil.projectDir, global = true)

                if (projectDir != null && projectDir.exists()) {
                    isRepository = gitService.isRepository(projectDir)
                    if (isRepository) {
                        gitService.getStatus(projectDir)
                            .onSuccess { status = it }
                            .onFailure { status = GitStatus() }

                        gitService.getBranches(projectDir)
                            .onSuccess { branches = it }
                            .onFailure { branches = emptyList() }

                        gitService.getRemotes(projectDir)
                            .onSuccess { remotes = it }
                            .onFailure { remotes = emptyList() }

                        gitService.getLog(projectDir)
                            .onSuccess { commits = it }
                            .onFailure { commits = emptyList() }

                        localConfig = gitService.getUserConfig(projectDir, global = false)
                    } else {
                        status = GitStatus()
                        branches = emptyList()
                        remotes = emptyList()
                        commits = emptyList()
                        localConfig = GitUserConfig()
                    }
                } else {
                    isRepository = false
                }
            } finally {
                operating = false
            }
        }
    }

    LaunchedEffect(initialProjectPath) {
        withContext(Dispatchers.IO) {
            val baseDir = initialProjectPath?.let { File(it) }
                ?: selectedProjectDir
                ?: FileUtil.projectDir.listFiles()
                    ?.firstOrNull { it.isDirectory && !it.name.startsWith(".") }
                ?: FileUtil.projectDir

            if (baseDir.exists()) {
                val projects = discoverProjectAndSubprojects(baseDir)
                availableProjects = projects
                val current = selectedProjectDir
                if (current == null || current !in projects) {
                    selectedProjectDir = projects.firstOrNull()
                }
            } else {
                availableProjects = emptyList()
            }
        }
    }

    LaunchedEffect(selectedProjectDir) {
        refreshRepoData(selectedProjectDir)
    }

    fun showMessage(message: String) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    fun runAsyncOperation(actionName: String, block: suspend () -> Unit) {
        scope.launch {
            operating = true
            try {
                block()
                refreshRepoData(selectedProjectDir)
                showMessage("$actionName completed successfully")
            } catch (error: Throwable) {
                showMessage(error.message ?: "$actionName failed")
            } finally {
                operating = false
            }
        }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "Git",
                            style = MaterialTheme.typography.headlineMediumEmphasized
                        )
                    },
                    navigationIcon = {
                        if (navBack != null) {
                            IconButton(onClick = navBack) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back"
                                )
                            }
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                )

                if (availableProjects.isNotEmpty()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ExposedDropdownMenuBox(
                                expanded = isProjectDropdownExpanded,
                                onExpandedChange = { isProjectDropdownExpanded = it },
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = selectedProjectDir?.name ?: "No project selected",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Project") },
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(
                                            expanded = isProjectDropdownExpanded
                                        )
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                        .fillMaxWidth()
                                )
                                ExposedDropdownMenu(
                                    expanded = isProjectDropdownExpanded,
                                    onDismissRequest = { isProjectDropdownExpanded = false }
                                ) {
                                    availableProjects.forEach { project ->
                                        val isRoot = availableProjects.firstOrNull() == project
                                        val displayName = if (isRoot) {
                                            "${project.name} (Root)"
                                        } else {
                                            val rootDir = availableProjects.firstOrNull() ?: project
                                            val relative = project.relativeToOrSelf(rootDir).path
                                            if (relative.isBlank()) project.name else relative
                                        }
                                        DropdownMenuItem(
                                            text = { Text(displayName) },
                                            onClick = {
                                                selectedProjectDir = project
                                                isProjectDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                PrimaryScrollableTabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    edgePadding = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ) {
                    GitScreenTab.entries.forEach { tab ->
                        Tab(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            text = {
                                Text(
                                    text = if (tab == GitScreenTab.CHANGES && status.totalChangedCount > 0) {
                                        "${tab.label} (${status.totalChangedCount})"
                                    } else {
                                        tab.label
                                    },
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                )
                            }
                        )
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            if (operating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Tab Content
            Box(modifier = Modifier.weight(1f)) {
                val currentDir = selectedProjectDir
                if (currentDir != null && !isRepository && selectedTab != GitScreenTab.SETTINGS) {
                    // Uninitialized Repository State
                    UninitializedRepoState(
                        projectName = currentDir.name,
                        operating = operating,
                        onInitialize = {
                            runAsyncOperation("Initialize Git Repository") {
                                gitService.initRepository(currentDir)
                            }
                        }
                    )
                } else {
                    when (selectedTab) {
                        GitScreenTab.CHANGES -> {
                            GitChangesTab(
                                status = status,
                                operating = operating,
                                onStageAll = {
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Stage all changes") {
                                            gitService.stageAll(dir)
                                        }
                                    }
                                },
                                onUnstageAll = {
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Unstage all changes") {
                                            gitService.unstageAll(dir)
                                        }
                                    }
                                },
                                onStageFiles = { files ->
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Stage files") {
                                            gitService.stageFiles(dir, files)
                                        }
                                    }
                                },
                                onUnstageFiles = { files ->
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Unstage files") {
                                            gitService.unstageFiles(dir, files)
                                        }
                                    }
                                },
                                onCommit = { message ->
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Commit") {
                                            gitService.commit(dir, message)
                                        }
                                    }
                                },
                                onPull = {
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Pull") {
                                            gitService.pull(dir)
                                        }
                                    }
                                },
                                onPush = {
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Push") {
                                            gitService.push(dir)
                                        }
                                    }
                                },
                                onFetch = {
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Fetch") {
                                            gitService.fetch(dir)
                                        }
                                    }
                                },
                                onRefresh = { refreshRepoData(selectedProjectDir) }
                            )
                        }

                        GitScreenTab.BRANCHES -> {
                            GitBranchesTab(
                                branches = branches,
                                remotes = remotes,
                                operating = operating,
                                onCheckoutBranch = { branchName ->
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Checkout $branchName") {
                                            gitService.checkoutBranch(dir, branchName)
                                        }
                                    }
                                },
                                onCreateBranch = { branchName ->
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Create branch $branchName") {
                                            gitService.createBranch(dir, branchName)
                                        }
                                    }
                                },
                                onDeleteBranch = { branchName ->
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Delete branch $branchName") {
                                            gitService.deleteBranch(dir, branchName)
                                        }
                                    }
                                },
                                onAddRemote = { name, url ->
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Add remote $name") {
                                            gitService.addRemote(dir, name, url)
                                        }
                                    }
                                },
                                onRemoveRemote = { name ->
                                    currentDir?.let { dir ->
                                        runAsyncOperation("Remove remote $name") {
                                            gitService.removeRemote(dir, name)
                                        }
                                    }
                                },
                                onRefresh = { refreshRepoData(selectedProjectDir) }
                            )
                        }

                        GitScreenTab.HISTORY -> {
                            GitHistoryTab(
                                commits = commits,
                                operating = operating,
                                onRefresh = { refreshRepoData(selectedProjectDir) }
                            )
                        }

                        GitScreenTab.SETTINGS -> {
                            GitSettingsTab(
                                globalConfig = globalConfig,
                                localConfig = localConfig,
                                hasLocalRepo = isRepository,
                                currentRepoPath = currentDir?.absolutePath,
                                gitVersion = gitVersion,
                                operating = operating,
                                onSaveConfig = { name, email, defaultBranch, isGlobal ->
                                    val dir = currentDir ?: FileUtil.projectDir
                                    runAsyncOperation("Save Git settings") {
                                        gitService.setUserConfig(
                                            workingDirectory = dir,
                                            name = name,
                                            email = email,
                                            defaultBranch = defaultBranch,
                                            global = isGlobal
                                        )
                                    }
                                },
                                onAddSafeDirectory = { path ->
                                    val dir = currentDir ?: FileUtil.projectDir
                                    runAsyncOperation("Add safe directory") {
                                        gitService.addSafeDirectory(dir, path)
                                    }
                                },
                                onSaveAuthToken = { host, username, token ->
                                    val dir = currentDir ?: FileUtil.projectDir
                                    runAsyncOperation("Save HTTPS authentication token") {
                                        gitService.saveAuthToken(
                                            workingDirectory = dir,
                                            host = host,
                                            username = username,
                                            token = token,
                                            global = true
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UninitializedRepoState(
    projectName: String,
    operating: Boolean,
    onInitialize: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
            ),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Hub,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Git Not Initialized",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "“$projectName” is not a Git repository yet. Initialize Git to track file changes, create branches, and push to remotes.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onInitialize,
                    enabled = !operating,
                    shapes = ButtonDefaults.shapes()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Initialize Git Repository")
                }
            }
        }
    }
}

internal fun discoverProjectAndSubprojects(targetDir: File): List<File> {
    if (!targetDir.exists()) return emptyList()

    val canonicalTarget = targetDir.canonicalFile
    val projectsDir = FileUtil.projectDir.canonicalFile

    val currentProjectRoot = when {
        canonicalTarget == projectsDir -> {
            projectsDir.listFiles()
                ?.filter { it.isDirectory && !it.name.startsWith(".") }
                ?.minByOrNull { it.name.lowercase() }
                ?: return emptyList()
        }

        canonicalTarget.parentFile == projectsDir -> canonicalTarget
        else -> {
            var curr = canonicalTarget
            while (curr.parentFile != null && curr.parentFile != projectsDir && curr != projectsDir) {
                curr = curr.parentFile!!
            }
            if (curr.parentFile == projectsDir) curr else canonicalTarget
        }
    }

    val result = mutableListOf<File>()
    result.add(currentProjectRoot)

    fun searchSubprojects(dir: File, depth: Int) {
        if (depth > 3) return
        val children = dir.listFiles()
            ?.filter { it.isDirectory && !it.name.startsWith(".") && it.name != "build" && it.name != "gradle" && it.name != "node_modules" }
            ?.sortedBy { it.name.lowercase() }
            ?: return

        for (child in children) {
            val isGitRepo = child.resolve(".git").exists()
            val hasBuildMarker = listOf(
                "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts",
                "Cargo.toml", "package.json", "pom.xml", "CMakeLists.txt", "Makefile"
            ).any { child.resolve(it).exists() }

            if (isGitRepo || hasBuildMarker) {
                result.add(child)
            }
            searchSubprojects(child, depth + 1)
        }
    }

    searchSubprojects(currentProjectRoot, depth = 1)

    return result.distinctBy { it.canonicalPath }
}
