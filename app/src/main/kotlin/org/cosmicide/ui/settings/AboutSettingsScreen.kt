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

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import org.cosmicide.BuildConfig
import org.cosmicide.R
import org.cosmicide.ui.donation.DonationSheet

private const val SourceUrl = "https://github.com/Cosmic-IDE/Cosmic-IDE"

@Composable
fun AboutSettingsScreen(
    gotoResourceScreen: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "About",
                        style = MaterialTheme.typography.headlineMediumEmphasized
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        }
    ) { scaffoldPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                ProductHeader()
            }

            item {
                ProjectActions(
                    onSource = { context.openUrl(SourceUrl) },
                )
            }

            item {
                SegmentedListItem(
                    onClick = gotoResourceScreen,
                    shapes = ListItemDefaults.segmentedShapes(0, 2),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            0.7f
                        )
                    ),
                    supportingContent = {
                        Text(
                            text = "Install or repair environment",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Handyman,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                ) {
                    Text(
                        text = "Repair Environment",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                SegmentedListItem(
                    onClick = { context.openStorageSettings() },
                    shapes = ListItemDefaults.segmentedShapes(1, 2),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            0.7f
                        )
                    ),
                    supportingContent = {
                        Text(
                            text = "Review Android File Access Permissions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingContent = {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Storage,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    },
                    trailingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                ) {
                    Text(
                        text = "Storage Access",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    "Build",
                    style = MaterialTheme.typography.titleMediumEmphasized,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                SegmentedListItem(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(0, 2),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            0.7f
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Version",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMediumEmphasized
                        )
                        Text(
                            text = BuildConfig.VERSION_NAME,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Default,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                SegmentedListItem(
                    onClick = {},
                    shapes = ListItemDefaults.segmentedShapes(1, 2),
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(
                            0.7f
                        )
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Revision",
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMediumEmphasized
                        )
                        Text(
                            text = BuildConfig.GIT_COMMIT,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Default,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Free and open source. GNU GPL v3.0",
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ProductHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(70.dp),
            shape = MaterialTheme.shapes.large,
            color = colorResource(R.color.logo_background)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = "Cosmic IDE logo",
                    modifier = Modifier.size(80.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cosmic IDE",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            style = MaterialTheme.typography.labelMediumEmphasized
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "A free and open-source development environment for Android.",
            modifier = Modifier.widthIn(max = 420.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}

@Composable
private fun ProjectActions(
    onSource: () -> Unit,
) {
    var showSheet by remember { mutableStateOf(false) }

    if (showSheet) {
        DonationSheet(onDismiss = { showSheet = false })
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FilledTonalButton(
            onClick = onSource,
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(
                Icons.Default.Code,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Source code", maxLines = 1, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(
            onClick = { showSheet = true },
            shapes = ButtonDefaults.shapes(),
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            contentPadding = ButtonDefaults.ButtonWithIconContentPadding
        ) {
            Icon(
                Icons.Filled.Favorite,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = Color.Red
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Support", maxLines = 1, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Context.openUrl(url: String) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
    }
}

private fun Context.openStorageSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        runCatching {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    "package:$packageName".toUri()
                )
            )
        }.getOrElse {
            startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
        }
    } else {
        startActivity(
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                "package:$packageName".toUri()
            )
        )
    }
}
