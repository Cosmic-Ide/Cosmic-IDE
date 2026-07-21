package org.cosmicide.plugin

import android.content.Context
import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.util.PreferenceKeys
import androidx.core.content.edit

class ExtensionSettings(context: Context) {
    private val preferences =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)

    fun isEnabled(extension: ConfigurableExtension): Boolean {
        if (!extension.canDisable) return true
        val key = key(extension.id)
        return if (preferences.contains(key)) {
            preferences.getBoolean(key, extension.enabledByDefault)
        } else {
            extension.enabledByDefault
        }
    }

    fun setEnabled(extension: ConfigurableExtension, enabled: Boolean) {
        require(extension.canDisable) { "Extension ${extension.id} cannot be disabled" }
        preferences.edit { putBoolean(key(extension.id), enabled) }
    }

    private fun key(extensionId: String): String {
        return PreferenceKeys.EXTENSION_ENABLED_PREFIX + extensionId
    }
}

data class ExtensionSettingsItem(
    val extension: ConfigurableExtension,
    val ownerPluginId: String,
    val category: String
)
