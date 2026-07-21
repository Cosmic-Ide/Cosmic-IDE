package org.cosmicide.editor.lsp

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.cosmicide.exec.ProcessExecutor
import org.cosmicide.editor.LspServerDefinition
import org.cosmicide.editor.LspServerProvider
import org.cosmicide.editor.LspServerRequest
import org.cosmicide.util.PreferenceKeys
import java.io.InputStream
import java.net.URI
import java.util.UUID

data class CustomLspConfiguration(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val fileExtension: String,
    val startScript: String,
    val textMateGrammarLink: String? = null,
    val enabled: Boolean = true
) {
    fun normalized(): CustomLspConfiguration {
        return copy(
            name = name.trim(),
            fileExtension = fileExtension.trim().removePrefix(".").lowercase(),
            startScript = startScript.trim(),
            textMateGrammarLink = textMateGrammarLink?.trim()?.ifBlank { null }
        )
    }

    fun validate() {
        require(name.isNotBlank()) { "Name is required" }
        require(FILE_EXTENSION.matches(fileExtension)) {
            "File type must be an extension such as rs or py"
        }
        require(startScript.isNotBlank()) { "Starter code is required" }
        textMateGrammarLink?.let { link ->
            val scheme = runCatching { URI(link).scheme?.lowercase() }.getOrNull()
            require(scheme in SUPPORTED_GRAMMAR_SCHEMES || link.startsWith('/')) {
                "Grammar must be an http(s), content, or file link, or an absolute path"
            }
        }
    }

    private companion object {
        val FILE_EXTENSION = Regex("[A-Za-z0-9][A-Za-z0-9_+-]*")
        val SUPPORTED_GRAMMAR_SCHEMES = setOf("http", "https", "content", "file")
    }
}

class CustomLspConfigurationStore(context: Context) {
    private val preferences =
        context.getSharedPreferences(context.packageName + "_preferences", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun configurations(): List<CustomLspConfiguration> {
        val json = preferences.getString(PreferenceKeys.CUSTOM_LSP_CONFIGURATIONS, null)
            ?: return emptyList()
        return runCatching {
            gson.fromJson<List<CustomLspConfiguration>>(json, CONFIGURATION_LIST_TYPE)
                .orEmpty()
                .map(CustomLspConfiguration::normalized)
                .withSingleActiveConfigurationPerExtension()
        }.onFailure {
            Log.w(TAG, "Ignoring invalid custom LSP configuration", it)
        }.getOrDefault(emptyList())
    }

    fun save(configuration: CustomLspConfiguration) {
        val normalized = configuration.normalized().also(CustomLspConfiguration::validate)
        val updated = configurations()
            .map { existing ->
                if (
                    normalized.enabled &&
                    existing.id != normalized.id &&
                    existing.fileExtension.equals(normalized.fileExtension, ignoreCase = true)
                ) {
                    existing.copy(enabled = false)
                } else {
                    existing
                }
            }
            .filterNot { it.id == normalized.id }
            .plus(normalized)
        write(updated)
    }

    fun remove(id: String) {
        write(configurations().filterNot { it.id == id })
    }

    private fun write(configurations: List<CustomLspConfiguration>) {
        preferences.edit {
            putString(PreferenceKeys.CUSTOM_LSP_CONFIGURATIONS, gson.toJson(configurations))
        }
    }

    private companion object {
        val CONFIGURATION_LIST_TYPE =
            object : TypeToken<List<CustomLspConfiguration>>() {}.type
        const val TAG = "CustomLspStore"
    }
}

private fun List<CustomLspConfiguration>.withSingleActiveConfigurationPerExtension():
        List<CustomLspConfiguration> {
    val activeExtensions = mutableSetOf<String>()
    return map { configuration ->
        if (configuration.enabled && !activeExtensions.add(configuration.fileExtension)) {
            configuration.copy(enabled = false)
        } else {
            configuration
        }
    }
}

class CustomLspServerProvider(
    private val context: Context,
    private val store: CustomLspConfigurationStore
) : LspServerProvider {
    override val id = "org.cosmicide.editor.lsp.custom"
    override val displayName = "Custom language servers"
    override val description = "Run user-configured language servers over standard input and output"
    override val priority = 500

    override fun supports(request: LspServerRequest): Boolean {
        return matchingConfiguration(request) != null
    }

    override fun createDefinition(request: LspServerRequest): LspServerDefinition {
        val configuration = matchingConfiguration(request)
            ?: error("No custom language server configured for .${request.extension}")
        return LspServerDefinition(
            id = "$id.${configuration.id}",
            fileExtension = configuration.fileExtension,
            displayName = configuration.name,
            connectionFactory = {
                ExistingProcessLspConnection {
                    ProcessExecutor.startCommand(
                        context = context,
                        command = "bash",
                        args = listOf("-c", configuration.startScript),
                        workingDir = request.project.root,
                        redirectErrorStream = false,
                        environmentOverrides = mapOf(
                            "COSMIC_PROJECT_ROOT" to request.project.root.absolutePath,
                            "COSMIC_FILE" to request.file.absolutePath,
                            "BASH_ENV" to context.filesDir.resolve("arch/home.bashrc").absolutePath
                        )
                    ).also { streamStderr(configuration.name, it.errorStream) }
                }
            },
            textMateGrammarLink = configuration.textMateGrammarLink,
            initializationTimeoutMillis = 120_000
        )
    }

    private fun matchingConfiguration(request: LspServerRequest): CustomLspConfiguration? {
        return store.configurations().firstOrNull {
            it.enabled && it.fileExtension.equals(request.extension, ignoreCase = true)
        }
    }

    private fun streamStderr(name: String, stderr: InputStream) {
        Thread {
            stderr.bufferedReader().useLines { lines ->
                lines.forEach { Log.d(TAG, "[$name] $it") }
            }
        }.apply {
            this.name = "Custom-LSP-Stderr"
            isDaemon = true
            start()
        }
    }

    private companion object {
        const val TAG = "CustomLspServer"
    }
}
