package org.cosmicide.ui.settings

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import org.cosmicide.common.Analytics
import org.cosmicide.ui.settings.components.EditTextPreference
import org.cosmicide.ui.settings.components.SliderPreference
import org.cosmicide.ui.settings.components.SwitchPreference
import org.cosmicide.util.PreferenceKeys

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferenceManager.getDefaultSharedPreferences(context) }

    var fontSize by remember { mutableFloatStateOf(prefs.getString(PreferenceKeys.EDITOR_FONT_SIZE, "12")?.toFloat() ?: 12f) }
    var tabSize by remember { mutableFloatStateOf(prefs.getInt(PreferenceKeys.EDITOR_TAB_SIZE, 4).toFloat()) }
    var jdtLs by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_JDT_LS, false)) }
    var editorFont by remember { mutableStateOf(prefs.getString(PreferenceKeys.EDITOR_FONT, "") ?: "") }
    var stickyScroll by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.STICKY_SCROLL, true)) }
    var useSpaces by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_USE_SPACES, true)) }
    var ligatures by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_LIGATURES_ENABLE, false)) }
    var wordWrap by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_WORDWRAP_ENABLE, false)) }
    var bracketAutocomplete by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.BRACKET_PAIR_AUTOCOMPLETE, true)) }
    var scrollbar by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_SCROLLBAR_SHOW, true)) }
    var quickDelete by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.QUICK_DELETE, true)) }
    var hwAccel by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_HW_ENABLE, true)) }
    var nonPrintable by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_NON_PRINTABLE_SYMBOLS_SHOW, false)) }
    var lineNumbers by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_LINE_NUMBERS_SHOW, false)) }
    var doubleClickClose by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.EDITOR_DOUBLE_CLICK_CLOSE, false)) }
    var disableSymbolsView by remember { mutableStateOf(prefs.getBoolean(PreferenceKeys.DISABLE_SYMBOLS_VIEW, false)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Code Editor") },
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
            SliderPreference(
                title = "Font size",
                summary = "Set the font size for the editor",
                value = fontSize,
                valueRange = 12f..32f,
                steps = 20,
                onValueChange = {
                    fontSize = it
                    prefs.edit { putFloat(PreferenceKeys.EDITOR_FONT_SIZE, it) }
                }
            )

            SliderPreference(
                title = "Tab size",
                summary = "Set the tab size for the editor",
                value = tabSize,
                valueRange = 2f..14f,
                steps = 12,
                onValueChange = {
                    tabSize = it
                    prefs.edit { putFloat(PreferenceKeys.EDITOR_TAB_SIZE, it) }
                }
            )

            SwitchPreference(
                title = "JDT LS",
                summary = "Uses JDT Language Server",
                checked = jdtLs,
                onCheckedChange = {
                    jdtLs = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_JDT_LS, it) }
                    Analytics.logEvent("experimental_java_completion", it)
                }
            )

            EditTextPreference(
                title = "Editor font",
                summary = "Enter the font path for editor",
                value = editorFont,
                onValueChange = {
                    editorFont = it
                    prefs.edit { putString(PreferenceKeys.EDITOR_FONT, it) }
                }
            )

            SwitchPreference(
                title = "Sticky scroll",
                summary = "Enables sticky scroll in the editor",
                checked = stickyScroll,
                onCheckedChange = {
                    stickyScroll = it
                    prefs.edit { putBoolean(PreferenceKeys.STICKY_SCROLL, it) }
                }
            )

            SwitchPreference(
                title = "Use spaces instead of tabs",
                summary = "Choose whether to use spaces instead of tab character",
                checked = useSpaces,
                onCheckedChange = {
                    useSpaces = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_USE_SPACES, it) }
                }
            )

            SwitchPreference(
                title = "Font ligatures",
                summary = "Enable & disable font ligatures",
                checked = ligatures,
                onCheckedChange = {
                    ligatures = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_LIGATURES_ENABLE, it) }
                }
            )

            SwitchPreference(
                title = "Word wrap",
                summary = "Enable & disable word wrap",
                checked = wordWrap,
                onCheckedChange = {
                    wordWrap = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_WORDWRAP_ENABLE, it) }
                }
            )

            SwitchPreference(
                title = "Bracket pair auto-completion",
                summary = "Enable & disable bracket pair auto-completion",
                checked = bracketAutocomplete,
                onCheckedChange = {
                    bracketAutocomplete = it
                    prefs.edit { putBoolean(PreferenceKeys.BRACKET_PAIR_AUTOCOMPLETE, it) }
                }
            )

            SwitchPreference(
                title = "Scrollbar",
                summary = "If enabled, shows scrollbar in the editor",
                checked = scrollbar,
                onCheckedChange = {
                    scrollbar = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_SCROLLBAR_SHOW, it) }
                }
            )

            SwitchPreference(
                title = "Fast delete blank lines",
                summary = "If enabled, blank lines are deleted quickly in the editor",
                checked = quickDelete,
                onCheckedChange = {
                    quickDelete = it
                    prefs.edit { putBoolean(PreferenceKeys.QUICK_DELETE, it) }
                }
            )

            SwitchPreference(
                title = "Hardware acceleration",
                summary = "Enabling this may result in increased memory usage, but will speed up editor rendering",
                checked = hwAccel,
                onCheckedChange = {
                    hwAccel = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_HW_ENABLE, it) }
                }
            )

            SwitchPreference(
                title = "Non-printable characters",
                summary = "If enabled, shows non-printable symbols in the editor",
                checked = nonPrintable,
                onCheckedChange = {
                    nonPrintable = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_NON_PRINTABLE_SYMBOLS_SHOW, it) }
                }
            )

            SwitchPreference(
                title = "Line numbers",
                summary = "If enabled, shows editor line numbers",
                checked = lineNumbers,
                onCheckedChange = {
                    lineNumbers = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_LINE_NUMBERS_SHOW, it) }
                }
            )

            SwitchPreference(
                title = "Double click to close",
                summary = "If enabled, double clicking on an opened tab will close it",
                checked = doubleClickClose,
                onCheckedChange = {
                    doubleClickClose = it
                    prefs.edit { putBoolean(PreferenceKeys.EDITOR_DOUBLE_CLICK_CLOSE, it) }
                }
            )

            SwitchPreference(
                title = "Disable symbols view",
                summary = "If enabled, symbols view above will be disabled",
                checked = disableSymbolsView,
                onCheckedChange = {
                    disableSymbolsView = it
                    prefs.edit { putBoolean(PreferenceKeys.DISABLE_SYMBOLS_VIEW, it) }
                }
            )
        }
    }
}
