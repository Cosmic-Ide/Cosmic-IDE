package org.cosmicide.ui.settings

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cosmicide.applyEditorThemeSelection
import org.cosmicide.common.Prefs
import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.ui.settings.components.PreferenceItem
import org.cosmicide.ui.settings.components.SingleChoicePreference
import org.cosmicide.ui.settings.components.SliderPreference
import org.cosmicide.ui.settings.components.SwitchPreference
import org.cosmicide.ui.theme.isDeviceInDarkTheme
import org.cosmicide.util.PreferenceKeys
import java.io.File
import kotlin.math.roundToInt

@Composable
fun EditorSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember {
        context.getSharedPreferences(
            context.packageName + "_preferences",
            Context.MODE_PRIVATE
        )
    }

    var fontSize by remember { mutableFloatStateOf(Prefs.editorFontSize) }
    var tabSize by remember { mutableFloatStateOf(Prefs.tabSize.toFloat()) }
    var editorTheme by remember { mutableStateOf(Prefs.editorTheme) }
    var editorFont by remember { mutableStateOf(Prefs.editorFont) }
    var fontSelectionError by remember { mutableStateOf<String?>(null) }
    var stickyScroll by remember { mutableStateOf(Prefs.stickyScroll) }
    var useSpaces by remember { mutableStateOf(Prefs.useSpaces) }
    var ligatures by remember { mutableStateOf(Prefs.useLigatures) }
    var wordWrap by remember { mutableStateOf(Prefs.wordWrap) }
    var bracketAutocomplete by remember { mutableStateOf(Prefs.bracketPairAutocomplete) }
    var scrollbar by remember { mutableStateOf(Prefs.scrollbarEnabled) }
    var quickDelete by remember { mutableStateOf(Prefs.quickDelete) }
    var hwAccel by remember { mutableStateOf(Prefs.hardwareAcceleration) }
    var nonPrintable by remember { mutableStateOf(Prefs.nonPrintableCharacters) }
    var lineNumbers by remember { mutableStateOf(Prefs.lineNumbers) }
    var minimap by remember { mutableStateOf(Prefs.minimap) }

    val fontPicker =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                scope.launch {
                    runCatching {
                        withContext(Dispatchers.IO) { installEditorFont(context, uri) }
                    }.onSuccess { path ->
                        editorFont = path
                        fontSelectionError = null
                        prefs.edit { putString(PreferenceKeys.EDITOR_FONT, path) }
                    }.onFailure { error ->
                        fontSelectionError = error.message ?: "Could not use the selected font"
                    }
                }
            }
        }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Code Editor",
                        style = MaterialTheme.typography.headlineMediumEmphasized
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Theme & Typography",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )

                    val themeThemes = remember {
                        CosmicPluginHost
                            .enabledExtensions(EditorExtensionPoints.THEME_PROVIDER)
                            .mapNotNull { provider ->
                                val theme = runCatching { provider.createTheme() }.getOrNull()
                                theme?.let { provider to it }
                            }
                    }
                    val themeOptions = remember(themeThemes) {
                        listOf(PreferenceKeys.EDITOR_THEME_AUTO to "Auto (Match System)") +
                                themeThemes.map { (provider, theme) ->
                                    theme.name to provider.displayName
                                }
                    }
                    val darkTheme = isDeviceInDarkTheme()
                    val currentThemeLabel = themeOptions
                        .firstOrNull { it.first == editorTheme }
                        ?.second
                        ?: editorTheme

                    SingleChoicePreference(
                        title = "Theme",
                        summary = currentThemeLabel,
                        selectedItem = editorTheme,
                        items = themeOptions,
                        onItemSelected = { value ->
                            editorTheme = value
                            prefs.edit { putString(PreferenceKeys.EDITOR_THEME, value) }
                            val registry = ThemeRegistry.getInstance()
                            val theme = themeThemes.firstOrNull { it.second.name == value }?.second
                            if (theme != null) {
                                runCatching { registry.loadTheme(theme) }
                            }
                            applyEditorThemeSelection(registry, darkTheme)
                        },
                        index = 0,
                        count = 4
                    )

                    SliderPreference(
                        title = "Font size",
                        summary = "Set the font size for the editor",
                        value = fontSize,
                        valueRange = 4f..32f,
                        steps = 27,
                        onValueChange = {
                            val rounded = it.roundToInt().toFloat()
                            fontSize = rounded
                            prefs.edit {
                                putString(
                                    PreferenceKeys.EDITOR_FONT_SIZE,
                                    rounded.toInt().toString()
                                )
                            }
                        },
                        index = 1,
                        count = 4
                    )

                    SliderPreference(
                        title = "Tab size",
                        summary = "Set the tab size for the editor",
                        value = tabSize,
                        valueRange = 2f..14f,
                        steps = 11,
                        onValueChange = {
                            val rounded = it.roundToInt().toFloat()
                            tabSize = rounded
                            prefs.edit {
                                putInt(PreferenceKeys.EDITOR_TAB_SIZE, rounded.toInt())
                            }
                        },
                        index = 2,
                        count = 4
                    )

                    PreferenceItem(
                        title = "Editor font",
                        summary = fontSelectionError ?: if (editorFont.isEmpty()) {
                            "Tap to choose a font file"
                        } else {
                            "Custom font selected • tap to replace"
                        },
                        trailingContent = if (editorFont.isNotEmpty()) {
                            {
                                TextButton(
                                    onClick = {
                                        editorFont = ""
                                        fontSelectionError = null
                                        prefs.edit { remove(PreferenceKeys.EDITOR_FONT) }
                                    }
                                ) {
                                    Text("Use bundled font")
                                }
                            }
                        } else null,
                        onClick = {
                            fontPicker.launch(
                                arrayOf(
                                    "font/*",
                                    "application/x-font-ttf",
                                    "application/x-font-opentype",
                                    "application/octet-stream"
                                )
                            )
                        },
                        index = 3,
                        count = 4
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Code Editing",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )

                    SwitchPreference(
                        title = "Sticky scroll",
                        summary = "Enables sticky scroll in the editor",
                        checked = stickyScroll,
                        onCheckedChange = {
                            stickyScroll = it
                            prefs.edit { putBoolean(PreferenceKeys.STICKY_SCROLL, it) }
                        },
                        index = 0,
                        count = 7
                    )

                    SwitchPreference(
                        title = "Use spaces instead of tabs",
                        summary = "Choose whether to use spaces instead of tab character",
                        checked = useSpaces,
                        onCheckedChange = {
                            useSpaces = it
                            prefs.edit { putBoolean(PreferenceKeys.EDITOR_USE_SPACES, it) }
                        },
                        index = 1,
                        count = 7
                    )

                    SwitchPreference(
                        title = "Font ligatures",
                        summary = "Enable & disable font ligatures",
                        checked = ligatures,
                        onCheckedChange = {
                            ligatures = it
                            prefs.edit { putBoolean(PreferenceKeys.EDITOR_LIGATURES_ENABLE, it) }
                        },
                        index = 2,
                        count = 7
                    )

                    SwitchPreference(
                        title = "Word wrap",
                        summary = "Enable & disable word wrap",
                        checked = wordWrap,
                        onCheckedChange = {
                            wordWrap = it
                            prefs.edit { putBoolean(PreferenceKeys.EDITOR_WORDWRAP_ENABLE, it) }
                        },
                        index = 3,
                        count = 7
                    )

                    SwitchPreference(
                        title = "Show Minimap",
                        summary = "Enable & disable Code Minimap",
                        checked = ligatures,
                        onCheckedChange = {
                            ligatures = it
                            prefs.edit { putBoolean(PreferenceKeys.SHOW_MINIMAP, it) }
                        },
                        index = 4,
                        count = 7
                    )

                    SwitchPreference(
                        title = "Bracket pair auto-completion",
                        summary = "Enable & disable bracket pair auto-completion",
                        checked = bracketAutocomplete,
                        onCheckedChange = {
                            bracketAutocomplete = it
                            prefs.edit { putBoolean(PreferenceKeys.BRACKET_PAIR_AUTOCOMPLETE, it) }
                        },
                        index = 5,
                        count = 7
                    )

                    SwitchPreference(
                        title = "Fast delete blank lines",
                        summary = "If enabled, blank lines are deleted quickly in the editor",
                        checked = quickDelete,
                        onCheckedChange = {
                            quickDelete = it
                            prefs.edit { putBoolean(PreferenceKeys.QUICK_DELETE, it) }
                        },
                        index = 6,
                        count = 7
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Display & Performance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                    )

                    SwitchPreference(
                        title = "Line numbers",
                        summary = "If enabled, shows editor line numbers",
                        checked = lineNumbers,
                        onCheckedChange = {
                            lineNumbers = it
                            prefs.edit { putBoolean(PreferenceKeys.EDITOR_LINE_NUMBERS_SHOW, it) }
                        },
                        index = 0,
                        count = 4
                    )

                    SwitchPreference(
                        title = "Scrollbar",
                        summary = "If enabled, shows scrollbar in the editor",
                        checked = scrollbar,
                        onCheckedChange = {
                            scrollbar = it
                            prefs.edit { putBoolean(PreferenceKeys.EDITOR_SCROLLBAR_SHOW, it) }
                        },
                        index = 1,
                        count = 4
                    )

                    SwitchPreference(
                        title = "Non-printable characters",
                        summary = "If enabled, shows non-printable symbols in the editor",
                        checked = nonPrintable,
                        onCheckedChange = {
                            nonPrintable = it
                            prefs.edit {
                                putBoolean(
                                    PreferenceKeys.EDITOR_NON_PRINTABLE_SYMBOLS_SHOW,
                                    it
                                )
                            }
                        },
                        index = 2,
                        count = 4
                    )

                    SwitchPreference(
                        title = "Hardware acceleration",
                        summary = "Enabling this may result in increased memory usage, but will speed up editor rendering",
                        checked = hwAccel,
                        onCheckedChange = {
                            hwAccel = it
                            prefs.edit { putBoolean(PreferenceKeys.EDITOR_HW_ENABLE, it) }
                        },
                        index = 3,
                        count = 4
                    )
                }
            }
        }
    }
}

private fun installEditorFont(context: Context, uri: Uri): String {
    val fontsDirectory = context.filesDir.resolve("fonts").apply { mkdirs() }
    val temporary = File(fontsDirectory, "editor-font.tmp")
    val destination = File(fontsDirectory, "editor-font")

    try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            temporary.outputStream().use(input::copyTo)
        } ?: error("The selected font could not be opened")

        Typeface.createFromFile(temporary)
        check(!destination.exists() || destination.delete()) {
            "Could not replace the existing editor font"
        }
        check(temporary.renameTo(destination)) {
            "Could not save the selected editor font"
        }
        return destination.absolutePath
    } catch (error: Throwable) {
        temporary.delete()
        throw error
    }
}
