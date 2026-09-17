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

    /**
     * Phase 1 ABI baseline: pins the JVM linkage surface preexisting compiled plugins depend
     * on. Full jar-level binary compatibility proof still requires a binary-compatibility
     * validator run against an old published artifact; this is a signature pin, not that proof.
     */
    @Test
    fun `descriptor constructor and copy signatures stay linkable`() {
        // Pin the canonical 11-parameter constructor (older compilers/synthetic variants exist).
        val constructor = PluginDescriptor::class.java.constructors
            .single { it.parameterCount == 11 }
        val erasedTypes = constructor.parameterTypes
        assertEquals(
            // id, name, version, entryClass, description, author, source: String,
            // classPath, dependencies: List, capabilities: Set, enabledByDefault: boolean
            listOf(
                String::class.java, String::class.java, String::class.java, String::class.java,
                String::class.java, String::class.java, String::class.java,
                List::class.java, List::class.java, Set::class.java, Boolean::class.javaPrimitiveType
            ),
            erasedTypes.toList()
        )

        val copy = PluginDescriptor::class.java.methods.single { it.name == "copy" }
        assertEquals(erasedTypes.toList(), copy.parameterTypes.toList())

        val getters = PluginDescriptor::class.java.methods.map { it.name }
        for (getter in listOf("getId", "getName", "getVersion", "getEntryClass", "getEnabledByDefault")) {
            assertTrue("missing getter $getter", getter in getters)
        }
    }

    private fun descriptor(
        id: String = "org.example.plugin",
        name: String = "Plugin",
        version: String = "1.0",
        entryClass: String = "org.example.Plugin"
    ) = PluginDescriptor(id, name, version, entryClass)
}
