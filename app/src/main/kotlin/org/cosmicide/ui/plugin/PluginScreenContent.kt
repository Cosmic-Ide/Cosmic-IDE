package org.cosmicide.ui.plugin

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.ui.UiExtensionPoints

val LocalPluginBackHandler = staticCompositionLocalOf<(() -> Unit)?> { null }

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

    CompositionLocalProvider(LocalPluginBackHandler provides onBack) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceContainer
        ) {
            if (provider != null) {
                provider.Content(args)
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Plugin screen not found: $pluginId/$screenId",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
