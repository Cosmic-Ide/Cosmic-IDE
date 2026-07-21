package org.cosmicide.plugin.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class PluginManifestReaderTest {
    @Test
    fun `missing manifest returns null`() {
        val root = Files.createTempDirectory("cosmic-plugin-manifest").toFile()
        try {
            assertNull(PluginManifestReader.read(root))
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
