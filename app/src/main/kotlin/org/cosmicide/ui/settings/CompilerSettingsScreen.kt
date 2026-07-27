/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import org.cosmicide.ui.settings.components.SingleChoicePreference
import org.cosmicide.util.PreferenceKeys
import org.cosmicide.util.jdkNames

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompilerSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember {
        context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }

    // Dynamic JDK Selection from org.cosmicide.util
    val installedJdkNames = remember { context.jdkNames() }
    var currentJdk by remember {
        mutableStateOf(prefs.getString("current_jdk", installedJdkNames.firstOrNull() ?: "None Installed") ?: "None Installed")
    }

    // Convert string array to Pair structure expected by SingleChoicePreference (value to label mapping)
    val jdkChoices = remember(installedJdkNames) {
        if (installedJdkNames.isEmpty()) {
            listOf("None Installed" to "No JDK variants found on disk")
        } else {
            installedJdkNames.map { it to it }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compiler") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Active Environment Toolchain",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 8.dp)
            )

            SingleChoicePreference(
                title = "Selected Active JDK",
                summary = "Currently using: $currentJdk",
                selectedItem = currentJdk,
                items = jdkChoices,
                onItemSelected = { selected ->
                    if (installedJdkNames.isNotEmpty()) {
                        currentJdk = selected
                        prefs.edit { putString(PreferenceKeys.COMPILER_CURRENT_JDK, selected) }
                    }
                }
            )
        }
    }
}