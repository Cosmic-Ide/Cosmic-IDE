/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.git.ui.tabs

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.cosmicide.plugin.git.GitBranch
import org.cosmicide.plugin.git.GitRemote

@Composable
fun GitBranchesTab(
    branches: List<GitBranch>,
    remotes: List<GitRemote>,
    operating: Boolean,
    onCheckoutBranch: (String) -> Unit,
    onCreateBranch: (String) -> Unit,
    onDeleteBranch: (String) -> Unit,
    onAddRemote: (name: String, url: String) -> Unit,
    onRemoveRemote: (name: String) -> Unit,
    onRefresh: () -> Unit
) {
    var showCreateBranchDialog by remember { mutableStateOf(false) }
    var showAddRemoteDialog by remember { mutableStateOf(false) }
    var branchToDelete by remember { mutableStateOf<GitBranch?>(null) }
    var remoteToDelete by remember { mutableStateOf<GitRemote?>(null) }

    val localBranches = branches.filter { !it.isRemote }
    val remoteBranches = branches.filter { it.isRemote }
    val currentBranch = localBranches.firstOrNull { it.isCurrent }

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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Current Branch",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.CallSplit,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = currentBranch?.name ?: "No branch",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = onRefresh,
                                enabled = !operating,
                                shapes = IconButtonDefaults.shapes()
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Refresh branches")
                            }
                            Button(
                                onClick = { showCreateBranchDialog = true },
                                enabled = !operating,
                                shapes = ButtonDefaults.shapes()
                            ) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text("New Branch")
                            }
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Local Branches (${localBranches.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    if (localBranches.isEmpty()) {
                        Text(
                            text = "No local branches found.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        localBranches.forEach { branch ->
                            LocalBranchRow(
                                branch = branch,
                                operating = operating,
                                onCheckout = { onCheckoutBranch(branch.name) },
                                onDelete = { branchToDelete = branch }
                            )
                        }
                    }
                }
            }
        }

        if (remoteBranches.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Remote Tracking Branches (${remoteBranches.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        remoteBranches.forEach { branch ->
                            RemoteBranchRow(
                                branch = branch,
                                operating = operating,
                                onCheckout = {
                                    val localName = branch.displayName.substringAfter('/')
                                    onCheckoutBranch(localName)
                                }
                            )
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remotes (${remotes.size})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(
                            onClick = { showAddRemoteDialog = true },
                            enabled = !operating
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Add remote")
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    if (remotes.isEmpty()) {
                        Text(
                            text = "No remote repositories configured.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        remotes.forEach { remote ->
                            RemoteRow(
                                remote = remote,
                                operating = operating,
                                onDelete = { remoteToDelete = remote }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCreateBranchDialog) {
        var branchName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateBranchDialog = false },
            title = { Text("Create & Checkout Branch") },
            text = {
                Column {
                    Text("Enter a new branch name to create from current revision:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = branchName,
                        onValueChange = { branchName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Branch name") },
                        placeholder = { Text("feature/my-feature") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = branchName.trim()
                        if (name.isNotBlank()) {
                            showCreateBranchDialog = false
                            onCreateBranch(name)
                        }
                    },
                    enabled = branchName.isNotBlank() && !operating
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateBranchDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Remote Dialog
    if (showAddRemoteDialog) {
        var remoteName by remember { mutableStateOf("origin") }
        var remoteUrl by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddRemoteDialog = false },
            title = { Text("Add Remote Repository") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = remoteName,
                        onValueChange = { remoteName = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Remote name") },
                        placeholder = { Text("origin") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = remoteUrl,
                        onValueChange = { remoteUrl = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Remote URL") },
                        placeholder = { Text("https://github.com/owner/repo.git") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = remoteName.trim()
                        val url = remoteUrl.trim()
                        if (name.isNotBlank() && url.isNotBlank()) {
                            showAddRemoteDialog = false
                            onAddRemote(name, url)
                        }
                    },
                    enabled = remoteName.isNotBlank() && remoteUrl.isNotBlank() && !operating
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRemoteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Branch Confirmation Dialog
    branchToDelete?.let { branch ->
        AlertDialog(
            onDismissRequest = { branchToDelete = null },
            title = { Text("Delete Branch?") },
            text = { Text("Are you sure you want to delete branch “${branch.name}”?") },
            confirmButton = {
                Button(
                    onClick = {
                        branchToDelete = null
                        onDeleteBranch(branch.name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { branchToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Remove Remote Confirmation Dialog
    remoteToDelete?.let { remote ->
        AlertDialog(
            onDismissRequest = { remoteToDelete = null },
            title = { Text("Remove Remote?") },
            text = { Text("Are you sure you want to remove remote “${remote.name}”?") },
            confirmButton = {
                Button(
                    onClick = {
                        remoteToDelete = null
                        onRemoveRemote(remote.name)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { remoteToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun LocalBranchRow(
    branch: GitBranch,
    operating: Boolean,
    onCheckout: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (branch.isCurrent) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Current branch",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        } else {
            Icon(
                Icons.AutoMirrored.Filled.CallSplit,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = branch.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (branch.isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = if (branch.isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (branch.upstream != null) {
                Text(
                    text = "tracks ${branch.upstream}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!branch.isCurrent) {
            FilledTonalButton(
                onClick = onCheckout,
                enabled = !operating,
                shapes = ButtonDefaults.shapes()
            ) {
                Text("Checkout")
            }
            Spacer(Modifier.width(4.dp))
            IconButton(
                onClick = onDelete,
                enabled = !operating,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete branch",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun RemoteBranchRow(
    branch: GitBranch,
    operating: Boolean,
    onCheckout: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Cloud,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = branch.displayName,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedButton(
            onClick = onCheckout,
            enabled = !operating,
            shapes = ButtonDefaults.shapes()
        ) {
            Text("Checkout")
        }
    }
}

@Composable
private fun RemoteRow(
    remote: GitRemote,
    operating: Boolean,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Language,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = remote.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = remote.fetchUrl,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(
            onClick = onDelete,
            enabled = !operating,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Remove remote",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
