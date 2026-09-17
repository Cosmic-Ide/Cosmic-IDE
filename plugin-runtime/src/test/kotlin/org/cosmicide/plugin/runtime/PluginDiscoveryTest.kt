package org.cosmicide.plugin.runtime

import org.cosmicide.plugin.api.PluginCompatibilityException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PluginDiscoveryTest {
    @Test
    fun `malformed package is isolated and discovery continues to later packages`() {
        val root = Files.createTempDirectory("cosmic-plugin-discovery").toFile()
        try {
            root.resolve("org.example.broken").apply { mkdirs() }
                .resolve(PluginManifestReader.MANIFEST_FILE).writeText("{ not json")
            root.resolve("org.example.legacy").apply { mkdirs() }
                .resolve(PluginManifestReader.MANIFEST_FILE).writeText(
                    """{"id":"org.example.legacy","entryClass":"org.example.Legacy","classPath":["plugin.apk"]}"""
                )
            root.resolve("org.example.incompatible").apply { mkdirs() }
                .resolve(PluginManifestReader.MANIFEST_FILE).writeText(
                    """{"id":"org.example.incompatible","entryClass":"org.example.Future","schemaVersion":1,"compatibility":{"pluginApi":{"minInclusive":"99.0.0","maxExclusive":"99.0.1"}}}"""
                )
            root.resolve(".hidden-staging").apply { mkdirs() }
                .resolve(PluginManifestReader.MANIFEST_FILE).writeText("{}")
            root.resolve("org.example.missing-manifest").apply { mkdirs() }

            val results = PluginDiscovery.discover(root)

            // .hidden-staging is excluded; the other four packages are all reported.
            assertEquals(4, results.size)
            val invalid = results.filterIsInstance<PluginDiscoveryResult.Invalid>()
            assertEquals(
                setOf("org.example.broken", "org.example.incompatible", "org.example.missing-manifest"),
                invalid.map { it.directory.name }.toSet()
            )
            assertEquals(
                "org.example.legacy",
                results.filterIsInstance<PluginDiscoveryResult.Valid>().single().directory.name
            )

            // The incompatible package receives a specific pre-load failure cause.
            val incompatible = invalid.first { it.directory.name == "org.example.incompatible" }
            assertTrue(incompatible.cause is PluginCompatibilityException)
            assertEquals(
                "Incompatible plugin API: host 1.0.0 is outside [99.0.0, 99.0.1)",
                incompatible.reason
            )
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `installed plugin reads are confined and revalidated before loading`() {
        val root = Files.createTempDirectory("cosmic-plugin-discovery").toFile()
        try {
            val manifestText =
                """{"id":"org.example.confined","entryClass":"org.example.Confined"}"""
            val directory = root.resolve("org.example.confined").apply { mkdirs() }
            directory.resolve(PluginManifestReader.MANIFEST_FILE).writeText(manifestText)

            assertEquals(
                "org.example.confined",
                installedPluginDirectory(root, "org.example.confined").name
            )
            // Callers pass a descriptor read from the same manifest earlier.
            val earlierRead = PluginManifestReader.readDescriptor(manifestText)
            assertEquals(
                "org.example.confined",
                validateInstalledPlugin(root, earlierRead).descriptor.id
            )

            for (id in listOf("..", "../escape", ".staging", "bad/id", "")) {
                try {
                    installedPluginDirectory(root, id)
                    throw AssertionError("expected confinement rejection for '$id'")
                } catch (_: IllegalArgumentException) {
                }
            }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `revalidation detects replaced manifests and incompatible ranges before code loads`() {
        val root = Files.createTempDirectory("cosmic-plugin-discovery").toFile()
        try {
            val descriptor = PluginManifestReader.readDescriptor(
                """{"id":"org.example.swap","entryClass":"org.example.Swap"}"""
            )
            val directory = root.resolve("org.example.swap").apply { mkdirs() }
            val manifest = directory.resolve(PluginManifestReader.MANIFEST_FILE)

            manifest.writeText("""{"id":"org.example.other","entryClass":"org.example.Other"}""")
            try {
                validateInstalledPlugin(root, descriptor)
                throw AssertionError("expected descriptor mismatch rejection")
            } catch (_: IllegalArgumentException) {
            }

            manifest.writeText(
                """{"id":"org.example.swap","entryClass":"org.example.Swap","schemaVersion":1,"compatibility":{"ideApi":{"minInclusive":"99.0.0","maxExclusive":"99.0.1"}}}"""
            )
            try {
                validateInstalledPlugin(root, descriptor)
                throw AssertionError("expected incompatible IDE API rejection")
            } catch (expected: PluginCompatibilityException) {
                assertTrue(expected.message!!.contains("IDE API: host 1.0.0"))
            }
        } finally {
            root.deleteRecursively()
        }
    }
}
