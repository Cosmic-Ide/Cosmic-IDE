/*
 * Copyright (C) 2024 Pranav Purwar <purwarpranav80@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Hub
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cosmicide.R
import org.cosmicide.ui.SettingsDestination

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToCategory: (SettingsCategory) -> Unit,
    onNavigateToPluginSettings: (String) -> Unit = {}
) {
    val categories = remember {
        listOf(
            SettingsCategory.Editor,
            SettingsCategory.Compiler,
            SettingsCategory.Extensions,
            SettingsCategory.Terminal,
            SettingsCategory.Toolchains,
            SettingsCategory.About
        )
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.action_settings),
                        style = MaterialTheme.typography.headlineMediumEmphasized
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack, shapes = IconButtonDefaults.shapes()
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            itemsIndexed(categories, key = { _, it -> it.destination.name }) { index, category ->
                SettingsCategoryItem(
                    category = category,
                    index = index,
                    count = categories.size,
                    onClick = { onNavigateToCategory(category) })
            }

            val pluginSettings =
                org.cosmicide.plugin.CosmicPluginHost.enabledExtensions(org.cosmicide.ui.UiExtensionPoints.SETTINGS_UI)
            if (pluginSettings.isNotEmpty()) {
                item {
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Plugins",
                        style = MaterialTheme.typography.titleMediumEmphasized,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                    )
                }
                itemsIndexed(pluginSettings, key = { _, it -> it.id }) { index, provider ->
                    SegmentedListItem(
                        onClick = { onNavigateToPluginSettings(provider.id) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.segmentedColors(
                            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                        ),
                        verticalAlignment = Alignment.CenterVertically,
                        shapes = ListItemDefaults.segmentedShapes(index, pluginSettings.size),
                        content = {
                            Text(
                                text = provider.label,
                                style = MaterialTheme.typography.titleMediumEmphasized,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        },
                        leadingContent = {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Extension,
                                    contentDescription = null,
                                    modifier = Modifier.size(30.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }
    }
}

sealed class SettingsCategory(
    val destination: SettingsDestination,
    val title: String,
    val summary: String,
    val icon: ImageVector
) {
    data object Editor : SettingsCategory(
        SettingsDestination.EDITOR,
        "Code editor",
        "Customize theme, fonts, formatting, and rendering",
        Icons.Default.Code
    )

    data object Compiler : SettingsCategory(
        SettingsDestination.COMPILER,
        "Compiler",
        "Configure compiler options and build toolchains",
        Icons.Default.Build
    )

    data object Extensions : SettingsCategory(
        SettingsDestination.EXTENSIONS,
        "Extensions",
        "Manage providers, plugins, and language servers",
        Icons.Default.Hub
    )

    data object Terminal : SettingsCategory(
        SettingsDestination.TERMINAL,
        "Terminal",
        "Configure shell, fonts, and terminal behaviors",
        Icons.Default.Terminal
    )

    data object Toolchains : SettingsCategory(
        SettingsDestination.TOOLCHAINS,
        "Toolchains & SDKs",
        "Manage installed JDKs and native toolchains",
        Icons.Default.Hardware
    )

    data object About : SettingsCategory(
        SettingsDestination.ABOUT, "About", "App info, license, and resources", Icons.Default.Info
    )
}

@Composable
fun SettingsCategoryItem(
    category: SettingsCategory, index: Int, count: Int, onClick: () -> Unit
) {
    SegmentedListItem(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = ListItemDefaults.segmentedColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
        ),
        verticalAlignment = Alignment.CenterVertically,
        shapes = ListItemDefaults.segmentedShapes(index, count),
        content = {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMediumEmphasized,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                text = category.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        leadingContent = {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        })
}
