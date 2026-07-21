package org.cosmicide.ui.settings.extensions

import org.cosmicide.editor.lsp.CustomLspConfiguration
import org.cosmicide.plugin.customproject.CustomProjectCommandConfiguration
import org.cosmicide.plugin.customproject.CustomProjectTypeConfiguration
import java.util.UUID

internal fun buildCustomLspConfiguration(
    existing: CustomLspConfiguration?,
    name: String,
    fileExtension: String,
    startScript: String,
    grammarLink: String,
    id: String = existing?.id ?: UUID.randomUUID().toString()
): CustomLspConfiguration = CustomLspConfiguration(
    id = id,
    name = name,
    fileExtension = fileExtension,
    startScript = startScript,
    textMateGrammarLink = grammarLink,
    enabled = existing?.enabled ?: true
).normalized().also(CustomLspConfiguration::validate)

internal fun buildCustomProjectTypeConfiguration(
    existing: CustomProjectTypeConfiguration?,
    name: String,
    markers: String,
    createCommand: String,
    syncCommand: String,
    buildCommand: String,
    runCommand: String,
    additionalCommands: String,
    id: String = existing?.id ?: UUID.randomUUID().toString()
): CustomProjectTypeConfiguration {
    val commands = additionalCommands.lineSequence()
        .map(String::trim)
        .filter(String::isNotEmpty)
        .map { line ->
            val parts = line.split("::", limit = 2)
            require(parts.size == 2) {
                "Additional commands must use Label :: shell code"
            }
            CustomProjectCommandConfiguration(
                name = parts[0],
                command = parts[1]
            )
        }
        .toList()

    return CustomProjectTypeConfiguration(
        id = id,
        name = name,
        markerFiles = markers.lines(),
        createCommand = createCommand,
        syncCommand = syncCommand,
        buildCommand = buildCommand,
        runCommand = runCommand,
        commands = commands,
        enabled = existing?.enabled ?: true
    ).normalized().also(CustomProjectTypeConfiguration::validate)
}
