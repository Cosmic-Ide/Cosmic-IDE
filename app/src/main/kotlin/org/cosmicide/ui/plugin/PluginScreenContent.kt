/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.ui.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.ui.UiExtensionPoints

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PluginScreenContent(
    pluginId: String,
    screenId: String,
    args: Map<String, String>,
    onBack: () -> Unit
) {
    val provider = remember(pluginId, screenId) {
        CosmicPluginHost.extensionRegistry.registrations(UiExtensionPoints.PLUGIN_SCREEN)
            .filter { it.ownerPluginId == pluginId && it.extension.screenId == screenId }
            .firstOrNull()?.extension
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(provider?.title ?: "Plugin Screen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (provider != null) {
                provider.Content()
            } else {
                Text(
                    text = "Plugin screen not found: $pluginId/$screenId",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
