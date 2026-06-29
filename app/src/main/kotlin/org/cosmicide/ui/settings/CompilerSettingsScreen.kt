/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.ui.settings

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.preference.PreferenceManager
import org.cosmicide.R
import org.cosmicide.ui.settings.components.EditTextPreference
import org.cosmicide.ui.settings.components.SingleChoicePreference
import org.cosmicide.ui.settings.components.SwitchPreference
import org.cosmicide.util.PreferenceKeys
import org.cosmicide.util.jdkNames
import org.jetbrains.kotlin.config.LanguageVersion
import androidx.core.content.edit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompilerSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    // Read general configurations
    var useFastJarFs by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.COMPILER_USE_FJFS, false)) }
    var useK2 by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.COMPILER_USE_K2, false)) }
    var javaVersion by remember { mutableStateOf(prefs.getString(PreferenceKeys.COMPILER_JAVA_VERSIONS, "27") ?: "27") }
    var kotlinVersion by remember { mutableStateOf(prefs.getString(PreferenceKeys.COMPILER_KOTLIN_VERSION, LanguageVersion.KOTLIN_2_4.versionString) ?: LanguageVersion.KOTLIN_2_4.versionString) }
    var javacFlags by remember { mutableStateOf(prefs.getString(PreferenceKeys.COMPILER_JAVAC_FLAGS, "") ?: "") }
    var repos by remember { mutableStateOf(prefs.getString("repos", "Maven Central: https://repo1.maven.org/maven2\nGoogle Maven: https://maven.google.com\nJitpack: https://jitpack.io\nSonatype Snapshots: https://s01.oss.sonatype.org/content/repositories/snapshots\nJCenter: https://jcenter.bintray.com") ?: "") }

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

    val javaVersions = (8..27).map { it.toString() to it.toString() }
    val kotlinVersions = listOf(
        LanguageVersion.KOTLIN_2_0,
        LanguageVersion.KOTLIN_2_1,
        LanguageVersion.KOTLIN_2_2,
        LanguageVersion.KOTLIN_2_3,
        LanguageVersion.KOTLIN_2_4,
        LanguageVersion.KOTLIN_2_5
    ).map { it.versionString to it.versionString }

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
                        Log.d("CompilerSettingsScreen", "Selected JDK: $selected")
                        prefs.edit { putString(PreferenceKeys.COMPILER_CURRENT_JDK, selected) }
                    }
                }
            )

            SingleChoicePreference(
                title = stringResource(R.string.java_version),
                summary = stringResource(R.string.java_version_desc),
                selectedItem = javaVersion,
                items = javaVersions,
                onItemSelected = {
                    javaVersion = it
                    prefs.edit().putString(PreferenceKeys.COMPILER_JAVA_VERSIONS, it).apply()
                }
            )

            Text(
                text = "Compiler Diagnostics & Tuning",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
            )

            SwitchPreference(
                title = stringResource(R.string.fast_jar_fs),
                summary = stringResource(R.string.experimental_caution),
                checked = useFastJarFs,
                onCheckedChange = {
                    useFastJarFs = it
                    prefs.edit().putBoolean(PreferenceKeys.COMPILER_USE_FJFS, it).apply()
                }
            )

            SwitchPreference(
                title = stringResource(R.string.k2_compiler),
                summary = stringResource(R.string.experimental_caution),
                checked = useK2,
                onCheckedChange = {
                    useK2 = it
                    prefs.edit().putBoolean(PreferenceKeys.COMPILER_USE_K2, it).apply()
                }
            )

            SingleChoicePreference(
                title = "Kotlin Version",
                summary = "Select the Kotlin version to use",
                selectedItem = kotlinVersion,
                items = kotlinVersions,
                onItemSelected = {
                    kotlinVersion = it
                    prefs.edit().putString(PreferenceKeys.COMPILER_KOTLIN_VERSION, it).apply()
                }
            )

            EditTextPreference(
                title = stringResource(R.string.additional_javac_flags),
                summary = stringResource(R.string.additional_javac_flags_desc),
                value = javacFlags,
                onValueChange = {
                    javacFlags = it
                    prefs.edit().putString(PreferenceKeys.COMPILER_JAVAC_FLAGS, it).apply()
                }
            )

            Text(
                text = "Library Download Manager",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 16.dp, top = 24.dp, end = 16.dp, bottom = 8.dp)
            )

            EditTextPreference(
                title = "Repositories",
                summary = "A list of repositories to search for libraries",
                value = repos,
                onValueChange = {
                    repos = it
                    prefs.edit().putString("repos", it).apply()
                }
            )
        }
    }
}