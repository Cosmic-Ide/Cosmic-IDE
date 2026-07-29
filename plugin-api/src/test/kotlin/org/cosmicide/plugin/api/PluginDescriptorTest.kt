package org.cosmicide.plugin.api

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PluginDescriptorTest {
    @Test
    fun `descriptor preserves plugin metadata`() {
        val descriptor = PluginDescriptor(
            id = "org.example.my-plugin_2",
            name = "Example",
            version = "2.1.0",
            entryClass = "org.example.Plugin",
            dependencies = listOf(PluginDependency("org.example.base", "2", optional = true)),
            capabilities = setOf("git", "projects")
        )

        assertEquals("org.example.my-plugin_2", descriptor.id)
        assertEquals(1, descriptor.dependencies.size)
        assertTrue("git" in descriptor.capabilities)
        assertTrue(descriptor.enabledByDefault)
    }

    @Test
    fun `descriptor rejects malformed required fields`() {
        listOf("", " ", "org/example/plugin", "plugin!").forEach { id ->
            assertFails<IllegalArgumentException> { descriptor(id = id) }
        }
        assertFails<IllegalArgumentException> { descriptor(name = " ") }
        assertFails<IllegalArgumentException> { descriptor(version = "") }
        assertFails<IllegalArgumentException> { descriptor(entryClass = " ") }
    }

    @Test
    fun `plugin setup actions require identity label and command`() {
        assertFails<IllegalArgumentException> {
            PluginSetupAction("", "Install", "pacman -S git")
        }
        assertFails<IllegalArgumentException> {
            PluginSetupAction("git", "", "pacman -S git")
        }
        assertFails<IllegalArgumentException> {
            PluginSetupAction("git", "Install", " ")
        }
    }

    private fun descriptor(
        id: String = "org.example.plugin",
        name: String = "Plugin",
        version: String = "1.0",
        entryClass: String = "org.example.Plugin"
    ) = PluginDescriptor(id, name, version, entryClass)
}
