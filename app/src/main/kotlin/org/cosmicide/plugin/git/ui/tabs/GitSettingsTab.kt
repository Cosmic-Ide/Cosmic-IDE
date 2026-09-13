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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import org.cosmicide.plugin.git.GitUserConfig

@Composable
fun GitSettingsTab(
    globalConfig: GitUserConfig,
    localConfig: GitUserConfig,
    hasLocalRepo: Boolean,
    currentRepoPath: String?,
    gitVersion: String,
    operating: Boolean,
    onSaveConfig: (name: String, email: String, defaultBranch: String, global: Boolean) -> Unit,
    onAddSafeDirectory: (String) -> Unit,
    onSaveAuthToken: (host: String, username: String, token: String) -> Unit = { _, _, _ -> }
) {
    var isGlobal by remember { mutableStateOf(!hasLocalRepo) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var defaultBranch by remember { mutableStateOf("") }

    val activeConfig = if (isGlobal) globalConfig else localConfig

    LaunchedEffect(isGlobal, globalConfig, localConfig) {
        name = activeConfig.name
        email = activeConfig.email
        defaultBranch = activeConfig.defaultBranch.ifBlank { "main" }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isGlobal,
                    onClick = { isGlobal = true },
                    label = { Text("Global settings") },
                    shapes = FilterChipDefaults.shapes()
                )
                if (hasLocalRepo) {
                    FilterChip(
                        selected = !isGlobal,
                        onClick = { isGlobal = false },
                        label = { Text("Project settings") },
                        shapes = FilterChipDefaults.shapes()
                    )
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
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isGlobal) "Global Git Identity" else "Project Git Identity",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = if (isGlobal) {
                            "These credentials will be used as the author on all Git commits unless overridden by a project."
                        } else {
                            "These credentials apply only to the active project repository."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Author name") },
                        placeholder = { Text("e.g. John Doe") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Author email") },
                        placeholder = { Text("e.g. john.doe@example.com") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = defaultBranch,
                        onValueChange = { defaultBranch = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Default branch name") },
                        placeholder = { Text("main") },
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onSaveConfig(name, email, defaultBranch, isGlobal)
                            },
                            enabled = !operating,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Save Configuration")
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
                var tokenHost by remember { mutableStateOf("github.com") }
                var tokenUser by remember { mutableStateOf("") }
                var tokenValue by remember { mutableStateOf("") }
                var tokenVisible by remember { mutableStateOf(false) }

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "HTTPS Authentication Token (PAT)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Text(
                        text = "Configure a Personal Access Token or credentials for remote repositories (e.g. GitHub/GitLab). This prevents git fetch, pull, or push operations from failing due to missing credentials.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    OutlinedTextField(
                        value = tokenHost,
                        onValueChange = { tokenHost = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Host / Domain") },
                        placeholder = { Text("github.com") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tokenUser,
                        onValueChange = { tokenUser = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Username (optional)") },
                        placeholder = { Text("e.g. octocat or x-access-token") },
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = tokenValue,
                        onValueChange = { tokenValue = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Personal Access Token / Password") },
                        placeholder = { Text("ghp_1234567890…") },
                        singleLine = true,
                        visualTransformation = if (tokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { tokenVisible = !tokenVisible }) {
                                Icon(
                                    imageVector = if (tokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (tokenVisible) "Hide token" else "Show token"
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(
                            onClick = {
                                onSaveAuthToken(tokenHost, tokenUser, tokenValue)
                                tokenValue = ""
                            },
                            enabled = !operating && tokenValue.isNotBlank(),
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Save Token")
                        }
                    }
                }
            }
        }

        // Safe Directory Card
        if (hasLocalRepo && currentRepoPath != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Security,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Safe Directory",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "If Git warns about dubious ownership for this project directory, add it to your safe directories list.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { onAddSafeDirectory(currentRepoPath) },
                            enabled = !operating,
                            shapes = ButtonDefaults.shapes()
                        ) {
                            Text("Mark Project Directory Safe")
                        }
                    }
                }
            }
        }

        // Toolchain & Environment Status Card
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(0.7f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Terminal,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Toolchain & Environment",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    val isInstalled = !gitVersion.contains("not found", ignoreCase = true)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (isInstalled) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isInstalled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = gitVersion,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Commands set GIT_TERMINAL_PROMPT=0 to prevent background processes from stalling on credentials. For private repositories, configure SSH keys or personal access tokens in remotes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
