/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.git.ui.tabs

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.cosmicide.plugin.git.GitFileStatus
import org.cosmicide.plugin.git.GitStatus

@Composable
fun GitChangesTab(
    status: GitStatus,
    operating: Boolean,
    onStageAll: () -> Unit,
    onUnstageAll: () -> Unit,
    onStageFiles: (List<String>) -> Unit,
    onUnstageFiles: (List<String>) -> Unit,
    onCommit: (String) -> Unit,
    onPull: () -> Unit,
    onPush: () -> Unit,
    onFetch: () -> Unit,
    onRefresh: () -> Unit
) {
    var commitMessage by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = status.branch,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (status.tracking != null) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        text = "→ ${status.tracking}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            if (status.ahead > 0 || status.behind > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.padding(top = 2.dp)
                                ) {
                                    if (status.ahead > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.ArrowUpward,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = "${status.ahead} ahead",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                    if (status.behind > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.ArrowDownward,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                            Text(
                                                text = "${status.behind} behind",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        IconButton(
                            onClick = onRefresh,
                            enabled = !operating,
                            shapes = IconButtonDefaults.shapes()
                        ) {
                            if (operating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh status")
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalButton(
                            onClick = onFetch,
                            enabled = !operating,
                            modifier = Modifier.weight(1f),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.Sync,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Fetch")
                        }
                        FilledTonalButton(
                            onClick = onPull,
                            enabled = !operating,
                            modifier = Modifier.weight(1f),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Pull")
                        }
                        Button(
                            onClick = onPush,
                            enabled = !operating,
                            modifier = Modifier.weight(1f),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Push")
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Commit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = commitMessage,
                        onValueChange = { commitMessage = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Commit message…") },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (status.staged.isEmpty() && (status.unstaged.isNotEmpty() || status.untracked.isNotEmpty())) {
                            OutlinedButton(
                                onClick = {
                                    onStageAll()
                                    if (commitMessage.isNotBlank()) {
                                        onCommit(commitMessage)
                                        commitMessage = ""
                                    }
                                },
                                enabled = !operating && commitMessage.isNotBlank(),
                                modifier = Modifier.padding(end = 8.dp),
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Text("Stage All & Commit")
                            }
                        }
                        Button(
                            onClick = {
                                if (commitMessage.isNotBlank()) {
                                    onCommit(commitMessage)
                                    commitMessage = ""
                                }
                            },
                            enabled = !operating && commitMessage.isNotBlank() && status.staged.isNotEmpty(),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text("Commit (${status.staged.size})")
                        }
                    }
                }
            }
        }

        if (status.staged.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Staged Changes (${status.staged.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(
                                onClick = onUnstageAll,
                                enabled = !operating
                            ) {
                                Text("Unstage all")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        status.staged.forEach { file ->
                            FileStatusRow(
                                file = file,
                                isStaged = true,
                                onAction = { onUnstageFiles(listOf(file.path)) },
                                operating = operating
                            )
                        }
                    }
                }
            }
        }

        val workingChanges = status.unstaged + status.untracked
        if (workingChanges.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Changes (${workingChanges.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = onStageAll,
                                enabled = !operating
                            ) {
                                Text("Stage all")
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        workingChanges.forEach { file ->
                            FileStatusRow(
                                file = file,
                                isStaged = false,
                                onAction = { onStageFiles(listOf(file.path)) },
                                operating = operating
                            )
                        }
                    }
                }
            }
        }

        if (status.isClean) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = "Working tree is clean",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "No unstaged or untracked changes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileStatusRow(
    file: GitFileStatus,
    isStaged: Boolean,
    onAction: () -> Unit,
    operating: Boolean
) {
    val (statusLabel, statusColor) = when {
        file.isUntracked -> "U" to Color(0xFF689F38)
        file.isAdded -> "A" to Color(0xFF2E7D32)
        file.isDeleted -> "D" to Color(0xFFC62828)
        file.isModified -> "M" to Color(0xFF1565C0)
        else -> "•" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = statusColor.copy(alpha = 0.15f),
            modifier = Modifier.size(24.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (file.path.contains("/")) {
                Text(
                    text = file.path.substringBeforeLast('/'),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(
            onClick = onAction,
            enabled = !operating,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                imageVector = if (isStaged) Icons.Default.Remove else Icons.Default.Add,
                contentDescription = if (isStaged) "Unstage" else "Stage",
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
