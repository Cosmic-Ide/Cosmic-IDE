package org.cosmicide.plugin.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import java.nio.file.Files

class PluginManifestReaderTest {
    @Test
    fun `missing manifest returns null`() {
        val root = Files.createTempDirectory("cosmic-plugin-manifest").toFile()
        try {
            assertNull(PluginManifestReader.read(root))
            assertNull(PluginManifestReader.readMetadata(root))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `minimal manifest receives stable defaults`() {
        val descriptor = PluginManifestReader.readDescriptor(
            """{"id":"org.example.plugin","entryClass":"org.example.Plugin"}"""
        )

        assertEquals("org.example.plugin", descriptor.name)
        assertEquals("0.0.0", descriptor.version)
        assertTrue(descriptor.enabledByDefault)
        assertTrue(descriptor.classPath.isEmpty())
        assertTrue(descriptor.dependencies.isEmpty())
    }

    @Test
    fun `full manifest combines artifact declarations and dependency forms`() {
        val descriptor = PluginManifestReader.readDescriptor(
            """
            {
              "id": "org.example.git",
              "name": "Git",
              "version": "2.0",
              "entryClass": "org.example.GitPlugin",
              "description": "Git operations",
              "author": "Cosmic",
              "source": "builtin",
              "classPath": ["main.jar", ""],
              "classpath": ["support.dex"],
              "artifact": "plugin.apk",
              "dependencies": [
                "org.example.core",
                {"id":"org.example.ui","minVersion":"3.1","optional":true}
              ],
              "capabilities": ["projects", "git", ""],
              "enabledByDefault": false
            }
            """.trimIndent()
        )

        assertEquals(listOf("main.jar", "support.dex", "plugin.apk"), descriptor.classPath)
        assertEquals(2, descriptor.dependencies.size)
        assertEquals("org.example.core", descriptor.dependencies[0].id)
        assertEquals("3.1", descriptor.dependencies[1].minVersion)
        assertTrue(descriptor.dependencies[1].optional)
        assertEquals(setOf("projects", "git"), descriptor.capabilities)
        assertFalse(descriptor.enabledByDefault)
    }

    @Test
    fun `metadata distinguishes legacy from version one without changing descriptors`() {
        val fields = "\"classPath\":[\"main.jar\"],\"classpath\":[\"support.dex\"],\"artifact\":\"plugin.apk\",\"dependencies\":[\"org.example.core\"],\"capabilities\":[\"git\"]"
        val legacy = PluginManifestReader.readMetadata(manifest(fields))
        val versioned = PluginManifestReader.readMetadata(manifest("\"schemaVersion\":1,$fields"))
        assertNull(legacy.schemaVersion)
        assertEquals(1, versioned.schemaVersion)
        assertTrue(versioned.requirements.hasUncheckedApi)
        assertTrue(legacy.requirements.hasUncheckedApi)
        assertEquals(legacy.descriptor, versioned.descriptor)
        assertEquals(versioned.descriptor, PluginManifestReader.readDescriptor(manifest("\"schemaVersion\":1,$fields")))

        val root = Files.createTempDirectory("cosmic-plugin-manifest").toFile()
        try {
            val file = root.resolve(PluginManifestReader.MANIFEST_FILE)
            file.writeText(manifest("\"schemaVersion\":1,$fields"))
            assertEquals(versioned, PluginManifestReader.readMetadata(root))
            assertEquals(versioned.descriptor, PluginManifestReader.read(root))
            file.writeText(manifest("\"schemaVersion\":2"))
            assertFails<IllegalArgumentException> { PluginManifestReader.read(root) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `malformed JSON still fails`() {
        assertFails<org.json.JSONException> {
            PluginManifestReader.readMetadata("{")
        }
    }

    @Test
    fun `explicit unsupported or malformed schema versions are rejected`() {
        for (version in listOf("0", "-1", "2", "2147483648", "1.0", "1e0", "\"1\"", "null", "true", "[]", "{}")) {
            assertFails<IllegalArgumentException> {
                PluginManifestReader.readDescriptor(manifest("\"schemaVersion\":$version"))
            }
        }
    }

    @Test
    fun `text byte limit accepts boundary and rejects one extra byte`() {
        val boundary = manifest().padEnd(64 * 1024, ' ')
        assertEquals("org.example.plugin", PluginManifestReader.readDescriptor(boundary).id)
        assertFails<IllegalArgumentException> {
            PluginManifestReader.readDescriptor(boundary + " ")
        }
    }

    @Test
    fun `text limit measures UTF8 bytes rather than characters`() {
        val json = manifest("\"description\":\"é😀\"")
        val boundary = json + " ".repeat(64 * 1024 - json.toByteArray(Charsets.UTF_8).size)
        assertTrue(boundary.length < 64 * 1024)
        assertEquals("é😀", PluginManifestReader.readDescriptor(boundary).description)
        assertFails<IllegalArgumentException> {
            PluginManifestReader.readDescriptor(boundary + " ")
        }
    }

    @Test
    fun `file byte limit accepts boundary and rejects oversized file before parsing`() {
        val root = Files.createTempDirectory("cosmic-plugin-manifest").toFile()
        try {
            val file = root.resolve(PluginManifestReader.MANIFEST_FILE)
            file.writeText(manifest().padEnd(64 * 1024, ' '))
            assertEquals("org.example.plugin", PluginManifestReader.read(root)?.id)
            file.appendText(" ")
            assertFails<IllegalArgumentException> { PluginManifestReader.read(root) }
            file.writeText("!".repeat(64 * 1024 + 1))
            assertFails<IllegalArgumentException> { PluginManifestReader.read(root) }
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `classpath limit combines both aliases and artifact`() {
        val entries = List(62) { "\"part$it.jar\"" }.joinToString(",")
        val fields = "\"classPath\":[$entries],\"classpath\":[\"support.dex\"],\"artifact\":\"plugin.apk\""
        assertEquals(64, PluginManifestReader.readDescriptor(manifest(fields)).classPath.size)
        assertFails<IllegalArgumentException> {
            PluginManifestReader.readDescriptor(manifest(fields.replace("support.dex\"", "support.dex\",\"extra.dex\"")))
        }
        for (alias in listOf("classPath", "classpath")) {
            assertFails<IllegalArgumentException> {
                PluginManifestReader.readDescriptor(manifest("\"$alias\":[${List(65) { "\"\"" }.joinToString(",")}]"))
            }
        }
    }

    @Test
    fun `dependency and capability limits count raw entries before filtering or deduplication`() {
        val dependencies = List(128) { "\"org.example.dep$it\"" }.joinToString(",")
        val capabilities = List(128) { "\"cap$it\"" }.joinToString(",")
        val descriptor = PluginManifestReader.readDescriptor(
            manifest("\"dependencies\":[$dependencies],\"capabilities\":[$capabilities]")
        )
        assertEquals(128, descriptor.dependencies.size)
        assertEquals(128, descriptor.capabilities.size)
        for ((field, entry) in listOf("dependencies" to "null", "capabilities" to "\"same\"", "capabilities" to "\"\"")) {
            assertFails<IllegalArgumentException> {
                PluginManifestReader.readDescriptor(manifest("\"$field\":[${List(129) { entry }.joinToString(",")}]"))
            }
        }
    }

    @Test
    fun `legacy optional field coercion and ignored dependency entries remain unchanged`() {
        val descriptor = PluginManifestReader.readDescriptor(
            manifest("\"classPath\":[42,\"\"],\"classpath\":false,\"dependencies\":[false,7,null],\"capabilities\":[\"same\",\"same\",\"\"]")
        )
        assertEquals(listOf("42"), descriptor.classPath)
        assertTrue(descriptor.dependencies.isEmpty())
        assertEquals(setOf("same"), descriptor.capabilities)
    }

    @Test
    fun `legacy manifests ignore compatibility object and keep unchecked mode`() {
        val metadata = PluginManifestReader.readMetadata(
            manifest("\"compatibility\":{\"pluginApi\":{\"minInclusive\":\"99.0.0\",\"maxExclusive\":\"99.0.1\"}}")
        )
        assertNull(metadata.schemaVersion)
        assertTrue(metadata.requirements.hasUncheckedApi)
        assertTrue(metadata.requireCompatible() === Unit)
    }

    @Test
    fun `versioned compatibility ranges are parsed and validated against host baselines`() {
        val metadata = PluginManifestReader.readMetadata(
            manifest("\"schemaVersion\":1,\"compatibility\":{\"pluginApi\":{\"minInclusive\":\"1.0.0\",\"maxExclusive\":\"2.0.0\"},\"ideApi\":{\"minInclusive\":\"0.1.0\",\"maxExclusive\":\"1.0.1\"}}")
        )
        assertEquals(1, metadata.schemaVersion)
        assertFalse(metadata.requirements.hasUncheckedApi)
        metadata.requireCompatible()
    }

    @Test
    fun `versioned incompatible or malformed compatibility ranges fail before loading`() {
        val incompatible = manifest("\"schemaVersion\":1,\"compatibility\":{\"pluginApi\":{\"minInclusive\":\"99.0.0\",\"maxExclusive\":\"99.0.1\"}}")
        val failure = assertFails<org.cosmicide.plugin.api.PluginCompatibilityException> {
            PluginManifestReader.readDescriptor(incompatible)
        }
        assertTrue(failure.message!!.contains("plugin API: host 1.0.0"))

        for (fields in listOf(
            "\"schemaVersion\":1,\"compatibility\":\"legacy\"",
            "\"schemaVersion\":1,\"compatibility\":{\"pluginApi\":\"1.0.0\"}",
            "\"schemaVersion\":1,\"compatibility\":{\"ideApi\":{\"minInclusive\":\"1.0.0\"}}",
            "\"schemaVersion\":1,\"compatibility\":{\"pluginApi\":{\"minInclusive\":\"1.0.0\",\"maxExclusive\":\"1.0.0\"}}",
            "\"schemaVersion\":1,\"compatibility\":{\"pluginApi\":{\"minInclusive\":\"2.0.0\",\"maxExclusive\":\"1.0.0\"}}",
            "\"schemaVersion\":1,\"compatibility\":{\"pluginApi\":{\"minInclusive\":\"1.x\",\"maxExclusive\":\"2.0.0\"}}",
            "\"schemaVersion\":1,\"compatibility\":{\"pluginApi\":{\"minInclusive\":\"01.0.0\",\"maxExclusive\":\"2.0.0\"}}"
        )) {
            assertFails<IllegalArgumentException> {
                PluginManifestReader.readDescriptor(manifest(fields))
            }
        }
    }

    private fun manifest(fields: String = ""): String =
        """{"id":"org.example.plugin","entryClass":"org.example.Plugin"${if (fields.isEmpty()) "" else ",$fields"}}"""

    @Test
    fun `invalid descriptor data fails instead of creating unusable plugin`() {
        assertFails<IllegalArgumentException> {
            PluginManifestReader.readDescriptor(
                """{"id":"bad/id","entryClass":"Plugin"}"""
            )
        }
        assertFails<org.json.JSONException> {
            PluginManifestReader.readDescriptor("""{"id":"org.example.missing"}""")
        }
    }
}

internal inline fun <reified T : Throwable> assertFails(block: () -> Unit): T {
    try {
        block()
    } catch (error: Throwable) {
        if (error is T) return error
        throw AssertionError("Expected ${T::class.java.name}, got ${error::class.java.name}", error)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
