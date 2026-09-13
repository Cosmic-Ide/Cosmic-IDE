package org.cosmicide.ui.home

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import me.saket.cascade.CascadeDropdownMenu
import org.cosmicide.R
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectAction
import org.cosmicide.project.ProjectActionProvider
import java.io.File
import java.util.Date

internal data class ProjectActionContribution(
    val provider: ProjectActionProvider,
    val action: ProjectAction,
    val project: Project,
)

internal fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    if (diff < 0) return "Just now"
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7
    val months = days / 30

    return when {
        seconds < 60 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "Yesterday"
        days < 7 -> "${days}d ago"
        weeks < 4 -> "${weeks}w ago"
        months < 12 -> "${months}mo ago"
        else -> {
            val date = Date(timestamp)
            java.text.SimpleDateFormat.getDateInstance(java.text.SimpleDateFormat.SHORT)
                .format(date)
        }
    }
}

@Composable
internal fun ProjectBadge(
    project: Project,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    iconUpdateTrigger: Int = 0,
    onClick: (() -> Unit)? = null,
) {
    val customIconFile = remember(project.root, iconUpdateTrigger) {
        val file = File(project.root, ".cosmic/icon.png")
        if ((file.exists()) && (file.length() > 0)) file else null
    }

    val customBitmap = remember(customIconFile) {
        customIconFile?.let { file ->
            runCatching {
                BitmapFactory.decodeFile(file.absolutePath)?.asImageBitmap()
            }.getOrNull()
        }
    }

    val initialLetter = remember(project.name) {
        project.name.trimStart('.', '_').firstOrNull()?.uppercaseChar()?.toString() ?: "P"
    }

    val shape = RoundedCornerShape(size * 0.28f)

    Surface(
        modifier = modifier
            .size(size)
            .clip(shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.primary,
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            if (customBitmap != null) {
                Image(
                    bitmap = customBitmap,
                    contentDescription = "${project.name} icon",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Text(
                    text = initialLetter,
                    style = if (size > 48.dp) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
internal fun ProjectCard(
    project: Project,
    index: Int,
    totalCount: Int,
    pluginActions: List<ProjectActionContribution> = emptyList(),
    onPluginAction: (ProjectActionContribution) -> Unit = {},
    onChangeIcon: () -> Unit = {},
    onResetIcon: () -> Unit = {},
    iconUpdateTrigger: Int = 0,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    val relativeTime = remember(project.root) {
        formatRelativeTime(project.root.lastModified())
    }
    val hasCustomIcon = remember(project.root, iconUpdateTrigger) {
        File(project.root, ".cosmic/icon.png").exists()
    }

    SegmentedListItem(
        onClick = onClick,
        onLongClick = { showMenu = true },
        modifier = Modifier.fillMaxWidth(),
        shapes = ListItemDefaults.segmentedShapes(index, totalCount),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surface.copy(0.7f),
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        leadingContent = {
            ProjectBadge(
                project = project,
                size = 48.dp,
                iconUpdateTrigger = iconUpdateTrigger,
                onClick = onChangeIcon,
            )
        },
        supportingContent = {
            Text(
                text = relativeTime,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        trailingContent = {
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    shapes = IconButtonDefaults.shapes(),
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options for ${project.name}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                CascadeDropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                ) {
                    pluginActions.forEach { contribution ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    contribution.action.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            },
                            onClick = {
                                showMenu = false
                                onPluginAction(contribution)
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Change icon", style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            showMenu = false
                            onChangeIcon()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                    if (hasCustomIcon) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Reset icon",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            onClick = {
                                showMenu = false
                                onResetIcon()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.RestartAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", style = MaterialTheme.typography.bodyLarge) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete Project",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                    )
                }
            }
        },
    ) {
        Text(
            text = project.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun EmptyProjectsState(
    onCreateClick: () -> Unit,
    onImportClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(76.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 2.dp,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "Welcome to Cosmic IDE",
            style = MaterialTheme.typography.headlineSmallEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Full-featured mobile developer environment. Start a new project or import an existing codebase.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.largeIncreased)
                    .clickable(onClick = onCreateClick),
                shape = MaterialTheme.shapes.largeIncreased,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Create New Project",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = "Start from installed plugin generators & templates",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.largeIncreased)
                    .clickable(onClick = onImportClick),
                shape = MaterialTheme.shapes.largeIncreased,
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FileUpload,
                                contentDescription = null,
                                modifier = Modifier.size(24.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
internal fun AnalyticsDialog(onDismiss: () -> Unit, onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = {
            Text(
                stringResource(R.string.analytics_permission_title),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Text(
                stringResource(R.string.analytics_permission_message),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onAccept,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDecline,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text(stringResource(R.string.decline))
            }
        },
    )
}

@Composable
internal fun DeleteProjectDialog(project: Project, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = MaterialTheme.shapes.extraLarge,
        title = { Text("Delete Project", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Text(
                "Are you sure you want to permanently delete ${project.name}?",
                style = MaterialTheme.typography.bodyLarge,
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shapes = ButtonDefaults.shapes(),
            ) {
                Text("Cancel")
            }
        },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.fillMaxWidth(0.8f)
    )
}
