package org.cosmicide.plugin

import android.content.Context
import androidx.preference.PreferenceManager
import org.cosmicide.plugin.api.ConfigurableExtension
import org.cosmicide.util.PreferenceKeys

class ExtensionSettings(context: Context) {
    private val preferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

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
        preferences.edit().putBoolean(key(extension.id), enabled).apply()
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
