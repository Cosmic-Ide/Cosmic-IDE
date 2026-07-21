package org.cosmicide.ui.settings.extensions

import android.content.Context
import androidx.core.content.edit
import org.cosmicide.common.Prefs
import org.cosmicide.editor.lsp.CustomLspConfiguration
import org.cosmicide.editor.lsp.CustomLspConfigurationStore
import org.cosmicide.plugin.CosmicPluginHost
import org.cosmicide.plugin.ExtensionSettingsItem
import org.cosmicide.plugin.api.PluginHandle
import org.cosmicide.plugin.customproject.CustomProjectTypeConfiguration
import org.cosmicide.plugin.customproject.CustomProjectTypeStore
import org.cosmicide.util.PreferenceKeys

internal interface ExtensionsSettingsRepository {
    fun extensionItems(): List<ExtensionSettingsItem>

    fun isExtensionEnabled(item: ExtensionSettingsItem): Boolean

    fun setExtensionEnabled(item: ExtensionSettingsItem, enabled: Boolean)

    fun customLspConfigurations(): List<CustomLspConfiguration>

    fun saveCustomLsp(configuration: CustomLspConfiguration)

    fun removeCustomLsp(id: String)

    fun customProjectTypes(): List<CustomProjectTypeConfiguration>

    fun saveCustomProjectType(configuration: CustomProjectTypeConfiguration)

    fun removeCustomProjectType(id: String)

    fun installedPlugins(): List<PluginHandle>

    fun pluginRepository(): String

    fun setPluginRepository(repository: String)
}

internal class AndroidExtensionsSettingsRepository(context: Context) :
    ExtensionsSettingsRepository {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(
        appContext.packageName + "_preferences",
        Context.MODE_PRIVATE
    )
    private val customLspStore = CustomLspConfigurationStore(appContext)
    private val customProjectTypeStore = CustomProjectTypeStore(appContext)

    override fun extensionItems(): List<ExtensionSettingsItem> =
        CosmicPluginHost.configurableExtensions()

    override fun isExtensionEnabled(item: ExtensionSettingsItem): Boolean =
        CosmicPluginHost.extensionSettings.isEnabled(item.extension)

    override fun setExtensionEnabled(item: ExtensionSettingsItem, enabled: Boolean) {
        CosmicPluginHost.extensionSettings.setEnabled(item.extension, enabled)
    }

    override fun customLspConfigurations(): List<CustomLspConfiguration> =
        customLspStore.configurations()

    override fun saveCustomLsp(configuration: CustomLspConfiguration) {
        customLspStore.save(configuration)
    }

    override fun removeCustomLsp(id: String) {
        customLspStore.remove(id)
    }

    override fun customProjectTypes(): List<CustomProjectTypeConfiguration> =
        customProjectTypeStore.configurations()

    override fun saveCustomProjectType(configuration: CustomProjectTypeConfiguration) {
        customProjectTypeStore.save(configuration)
    }

    override fun removeCustomProjectType(id: String) {
        customProjectTypeStore.remove(id)
    }

    override fun installedPlugins(): List<PluginHandle> =
        CosmicPluginHost.pluginManager?.plugins.orEmpty()

    override fun pluginRepository(): String = preferences.getString(
        PreferenceKeys.PLUGIN_REPOSITORY,
        Prefs.pluginRepository
    ) ?: Prefs.pluginRepository

    override fun setPluginRepository(repository: String) {
        preferences.edit { putString(PreferenceKeys.PLUGIN_REPOSITORY, repository) }
    }
}
