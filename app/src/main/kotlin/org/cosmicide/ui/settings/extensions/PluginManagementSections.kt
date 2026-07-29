package org.cosmicide.ui.settings.extensions

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import kotlinx.coroutines.launch
import org.cosmicide.plugin.PluginRepositoryEntry
import org.cosmicide.plugin.api.PluginHandle
import org.cosmicide.plugin.api.PluginSetupAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PluginMarketplaceSection(
    repository: ExtensionsSettingsRepository,
    refreshVersion: Int,
    onChanged: () -> Unit,
    onRunSetupInTerminal: (String) -> Unit
) {
    val scope = rememberCoroutineScope()
    val installed = remember(refreshVersion) {
        repository.installedPlugins().associateBy { it.descriptor.id }
    }
    var available by remember { mutableStateOf<List<PluginRepositoryEntry>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var query by remember { mutableStateOf("") }
    var showInstalledOnly by remember { mutableStateOf(false) }
    var operatingPluginId by remember { mutableStateOf<String?>(null) }
    var selectedPlugin by remember { mutableStateOf<PluginRepositoryEntry?>(null) }
    var pendingSetup by remember { mutableStateOf<PendingPluginSetup?>(null) }
    var pendingUninstall by remember { mutableStateOf<PluginRepositoryEntry?>(null) }
    var editRepository by remember { mutableStateOf(false) }

    fun refresh() {
        available = null
        errorMessage = null
        scope.launch {
            runCatching { repository.availablePlugins() }
                .onSuccess { available = it }
                .onFailure {
                    available = emptyList()
                    errorMessage = it.message ?: "Could not load the plugin repository"
                }
        }
    }

    fun install(plugin: PluginRepositoryEntry) {
        operatingPluginId = plugin.id
        errorMessage = null
        scope.launch {
            runCatching { repository.installPlugin(plugin) }
                .onSuccess { result ->
                    if (result.firstInstall && result.setupActions.isNotEmpty()) {
                        pendingSetup = PendingPluginSetup(plugin.name, result.setupActions)
                    }
                    onChanged()
                }
                .onFailure {
                    errorMessage = it.message ?: "Could not install ${plugin.name}"
                }
            operatingPluginId = null
        }
    }

    fun uninstall(plugin: PluginRepositoryEntry) {
        operatingPluginId = plugin.id
        errorMessage = null
        scope.launch {
            runCatching { repository.uninstallPlugin(plugin.id) }
                .onSuccess {
                    selectedPlugin = null
                    onChanged()
                }
                .onFailure {
                    errorMessage = it.message ?: "Could not uninstall ${plugin.name}"
                }
            operatingPluginId = null
        }
    }

    LaunchedEffect(refreshVersion, repository.pluginRepository()) {
        refresh()
    }

    val normalizedQuery = query.trim()
    val visiblePlugins = available.orEmpty().filter { plugin ->
        val matchesFilter = !showInstalledOnly || plugin.id in installed
        val matchesQuery = normalizedQuery.isEmpty() || listOf(
            plugin.name,
            plugin.author,
            plugin.id,
            plugin.shortDescription,
            plugin.detailedDescription
        ).any { it.contains(normalizedQuery, ignoreCase = true) }
        matchesFilter && matchesQuery
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search extensions") },
                colors = TextFieldDefaults.tonalColors(),
                shape = RoundedCornerShape(50),
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showInstalledOnly,
                    onClick = { showInstalledOnly = false },
                    label = { Text("Marketplace") }
                )
                FilterChip(
                    selected = showInstalledOnly,
                    onClick = { showInstalledOnly = true },
                    label = {
                        Text(
                            "Installed (${
                                available.orEmpty().count { it.id in installed }
                            })"
                        )
                    }
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = { editRepository = true }) {
                    Icon(Icons.Rounded.Settings, contentDescription = "Marketplace repository")
                }
                IconButton(onClick = ::refresh, enabled = available != null) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh marketplace")
                }
            }
        }

        when {
            available == null -> MarketplaceLoading()
            errorMessage != null && available.orEmpty().isEmpty() -> MarketplaceError(
                message = errorMessage.orEmpty(),
                onRetry = ::refresh
            )

            visiblePlugins.isEmpty() -> MarketplaceEmpty(
                message = when {
                    query.isNotBlank() -> "No extensions match “${query.trim()}”."
                    showInstalledOnly -> "No marketplace extensions are installed."
                    else -> "No extensions are currently published."
                }
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 2.dp,
                    bottom = 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                errorMessage?.let { message ->
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = message,
                                modifier = Modifier.padding(12.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                items(visiblePlugins, key = PluginRepositoryEntry::id) { plugin ->
                    PluginMarketplaceCard(
                        plugin = plugin,
                        installedPlugin = installed[plugin.id],
                        onClick = { selectedPlugin = plugin }
                    )
                }
            }
        }
    }

    selectedPlugin?.let { plugin ->
        PluginDetailsSheet(
            plugin = plugin,
            installedPlugin = installed[plugin.id],
            operating = operatingPluginId == plugin.id,
            onDismiss = { selectedPlugin = null },
            onInstall = { install(plugin) },
            onUninstall = { pendingUninstall = plugin },
            onRunSetup = installed[plugin.id]?.setupActions
                ?.takeIf(List<PluginSetupAction>::isNotEmpty)
                ?.let { actions ->
                    { pendingSetup = PendingPluginSetup(plugin.name, actions) }
                }
        )
    }

    pendingUninstall?.let { plugin ->
        AlertDialog(
            onDismissRequest = { pendingUninstall = null },
            title = { Text("Uninstall ${plugin.name}?") },
            text = {
                Text("The extension and its downloaded files will be removed from Cosmic IDE.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingUninstall = null
                        uninstall(plugin)
                    }
                ) {
                    Text("Uninstall", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUninstall = null }) { Text("Cancel") }
            }
        )
    }

    pendingSetup?.let { setup ->
        PluginSetupDialog(
            setup = setup,
            onDismiss = { pendingSetup = null },
            onRunSetupInTerminal = onRunSetupInTerminal
        )
    }

    if (editRepository) {
        PluginRepositoryDialog(
            currentRepository = repository.pluginRepository(),
            onDismiss = { editRepository = false },
            onSave = {
                repository.setPluginRepository(it)
                editRepository = false
                refresh()
            }
        )
    }
}

@Composable
private fun PluginMarketplaceCard(
    plugin: PluginRepositoryEntry,
    installedPlugin: PluginHandle?,
    onClick: () -> Unit
) {
    val hasUpdate = installedPlugin != null &&
            installedPlugin.descriptor.version != plugin.version
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PluginIcon(plugin.name)
            Spacer(Modifier.width(14.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = plugin.name,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (installedPlugin != null) {
                        Spacer(Modifier.width(8.dp))
                        PluginStatusLabel(if (hasUpdate) "Update" else "Installed")
                    }
                }
                Text(
                    text = plugin.shortDescription.ifBlank { "No description provided." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        if (plugin.author.isNotBlank()) append(plugin.author)
                        if (plugin.author.isNotBlank()) append(" · ")
                        append("v${plugin.version}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PluginIcon(name: String) {
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.trim().take(1).uppercase().ifBlank { "E" },
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

@Composable
private fun PluginStatusLabel(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PluginDetailsSheet(
    plugin: PluginRepositoryEntry,
    installedPlugin: PluginHandle?,
    operating: Boolean,
    onDismiss: () -> Unit,
    onInstall: () -> Unit,
    onUninstall: () -> Unit,
    onRunSetup: (() -> Unit)?
) {
    val uriHandler = LocalUriHandler.current
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)
    val hasUpdate = installedPlugin != null &&
            installedPlugin.descriptor.version != plugin.version

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                PluginIcon(plugin.name)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        plugin.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        buildString {
                            if (plugin.author.isNotBlank()) append(plugin.author)
                            if (plugin.author.isNotBlank()) append(" · ")
                            append("Version ${plugin.version}")
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close extension details")
                }
            }

            Text(
                text = plugin.shortDescription,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (installedPlugin != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = if (hasUpdate) {
                                "Version ${installedPlugin.descriptor.version} installed · " +
                                        "${plugin.version} available"
                            } else {
                                "Installed"
                            },
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (installedPlugin == null || hasUpdate) {
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !operating,
                        onClick = onInstall
                    ) {
                        OperationContent(
                            operating = operating,
                            label = if (hasUpdate) "Update" else "Install"
                        )
                    }
                }
                if (installedPlugin != null) {
                    FilledTonalButton(
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !operating,
                        onClick = onUninstall,
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text("Uninstall")
                    }
                }
                if (installedPlugin != null && onRunSetup != null) {
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onRunSetup,
                        shapes = ButtonDefaults.shapes(),
                    ) {
                        Text("Run setup")
                    }
                }
            }

            if (plugin.source.isNotBlank()) {
                TextButton(
                    onClick = { uriHandler.openUri(plugin.source) },
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("View source")
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                if (plugin.detailedDescription.isBlank()) {
                    Text(
                        "No detailed description was provided.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Markdown(
                        content = plugin.detailedDescription,
                        typography = markdownTypography(
                            h1 = MaterialTheme.typography.titleLargeEmphasized.copy(fontWeight = FontWeight.ExtraBold),
                            h2 = MaterialTheme.typography.titleLargeEmphasized.copy(fontWeight = FontWeight.Bold),
                            h3 = MaterialTheme.typography.titleMediumEmphasized.copy(fontWeight = FontWeight.Bold),
                            h4 = MaterialTheme.typography.titleSmallEmphasized.copy(fontWeight = FontWeight.Bold),
                            h5 = MaterialTheme.typography.headlineLargeEmphasized.copy(fontWeight = FontWeight.Bold),
                            h6 = MaterialTheme.typography.headlineMediumEmphasized.copy(fontWeight = FontWeight.Bold),
                            text = MaterialTheme.typography.bodyMedium,
                            code = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            inlineCode = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = TextUnit.Unspecified
                            ),
                            quote = MaterialTheme.typography.bodyMedium.plus(SpanStyle(fontStyle = FontStyle.Italic)),
                            paragraph = MaterialTheme.typography.bodyMedium,
                            ordered = MaterialTheme.typography.bodyMedium,
                            bullet = MaterialTheme.typography.bodyMedium,
                            list = MaterialTheme.typography.bodyMedium,
                            textLink = TextLinkStyles(
                                style = MaterialTheme.typography.bodyMediumEmphasized.copy(
                                    textDecoration = TextDecoration.Underline
                                ).toSpanStyle()
                            ),
                            table = MaterialTheme.typography.bodyMedium
                        )
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = plugin.id,
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OperationContent(operating: Boolean, label: String) {
    if (operating) {
        CircularProgressIndicator(
            modifier = Modifier
                .padding(end = 8.dp)
                .size(18.dp),
            strokeWidth = 2.dp
        )
    }
    Text(
        if (operating) {
            if (label == "Update") "Updating…" else "Installing…"
        } else {
            label
        }
    )
}

@Composable
private fun MarketplaceLoading() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(12.dp))
            Text("Loading marketplace…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MarketplaceError(message: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.Extension,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
private fun MarketplaceEmpty(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.Extension,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PluginSetupDialog(
    setup: PendingPluginSetup,
    onDismiss: () -> Unit,
    onRunSetupInTerminal: (String) -> Unit
) {
    val singleAction = setup.actions.singleOrNull()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set up ${setup.pluginName}") },
        modifier = Modifier.fillMaxWidth(0.8f),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Setup commands open in an interactive terminal and run only after you choose one.")
                setup.actions.forEach { action ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (setup.actions.size > 1) {
                            Text(action.label, style = MaterialTheme.typography.titleSmall)
                        }
                        if (action.description.isNotBlank()) {
                            Text(
                                action.description,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            action.command,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (setup.actions.size > 1) {
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    onDismiss()
                                    onRunSetupInTerminal(action.command)
                                }
                            ) {
                                Text(action.label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (singleAction != null) {
                TextButton(
                    onClick = {
                        onDismiss()
                        onRunSetupInTerminal(singleAction.command)
                    }
                ) { Text(singleAction.label) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Later") }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}

private data class PendingPluginSetup(
    val pluginName: String,
    val actions: List<PluginSetupAction>
)

@Composable
private fun PluginRepositoryDialog(
    currentRepository: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var value by remember(currentRepository) { mutableStateOf(currentRepository) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marketplace repository") },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("HTTPS index URL") },
                minLines = 2
            )
        },
        confirmButton = {
            TextButton(
                enabled = value.isNotBlank(),
                onClick = { onSave(value.trim()) }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
