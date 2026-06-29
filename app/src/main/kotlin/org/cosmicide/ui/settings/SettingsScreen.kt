package org.cosmicide.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.cosmicide.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onNavigateToCategory: (SettingsCategory) -> Unit
) {
    val categories = listOf(
        SettingsCategory.Appearance,
        SettingsCategory.Editor,
        SettingsCategory.Compiler,
        SettingsCategory.Formatter,
        SettingsCategory.Plugins,
        SettingsCategory.Git,
        SettingsCategory.Gemini,
        SettingsCategory.About
    )

    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.action_settings)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            items(categories) { category ->
                SettingsCategoryItem(category) {
                    onNavigateToCategory(category)
                }
            }
        }
    }
}

sealed class SettingsCategory(
    val title: String,
    val summary: String,
    val icon: ImageVector
) {
    data object Appearance : SettingsCategory("Appearance", "Customize the appearance as you see fit", Icons.Default.Palette)
    data object Editor : SettingsCategory("Code editor", "Customize editor settings", Icons.Default.Code)
    data object Compiler : SettingsCategory("Compiler", "Configure compiler options and build process", Icons.Default.Build)
    data object Formatter : SettingsCategory("Formatter", "Adjust code formatting preferences", Icons.Default.FormatAlignLeft)
    data object Plugins : SettingsCategory("Plugins", "Explore and install plugins", Icons.Default.Extension)
    data object Git : SettingsCategory("Git", "Configure Git integration", Icons.Default.Settings)
    data object Gemini : SettingsCategory("Gemini", "Configure Gemini integration", Icons.Default.Settings)
    data object About : SettingsCategory("About", "Learn more about Cosmic IDE", Icons.Default.Info)
}

@Composable
fun SettingsCategoryItem(
    category: SettingsCategory,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = category.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = category.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
