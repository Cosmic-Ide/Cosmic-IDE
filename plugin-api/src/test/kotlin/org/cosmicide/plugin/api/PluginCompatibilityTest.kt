package org.cosmicide.plugin.api

import org.junit.Assert.*
import org.junit.Test

class PluginCompatibilityTest {
    @Test fun `initial independent host baselines`() {
        assertEquals("1.0.0", PluginCompatibility.PLUGIN_API_VERSION)
        assertEquals("1.0.0", PluginCompatibility.IDE_API_VERSION)
        PluginCompatibility.requireCompatible(PluginApiRequirements())
        assertTrue(PluginApiRequirements().hasUncheckedApi)
    }

    @Test fun `range bounds are numeric inclusive minimum exclusive maximum`() {
        val range = HostApiRange("1.0.0", "2.0.0")
        assertTrue(range.contains("1.0.0"))
        assertTrue(range.contains("1.99.0"))
        assertFalse(range.contains("2.0.0"))
        assertFalse(range.contains("0.99.0"))
        assertTrue(HostApiRange("1.9.0", "1.11.0").contains("1.10.0"))
    }

    @Test fun `strict versions and ordered bounds`() {
        for (version in listOf("1", "1.0", "01.0.0", "1.0.0-alpha", "1.0.0+meta",
            " 1.0.0", "-1.0.0", "1.+1.0", "1.0.2147483648", "1.0.٠", "*")) {
            assertThrows(version, IllegalArgumentException::class.java) {
                PluginCompatibility.parseApiVersion(version)
            }
        }
        assertThrows(IllegalArgumentException::class.java) { HostApiRange("1.0.0", "1.0.0") }
        assertThrows(IllegalArgumentException::class.java) { HostApiRange("2.0.0", "1.0.0") }
    }

    @Test fun `future minor APIs are not claimed by initial host`() {
        val future = HostApiRange("1.1.0", "2.0.0")
        val pluginFailure = assertThrows(PluginCompatibilityException::class.java) {
            PluginCompatibility.requireCompatible(PluginApiRequirements(pluginApi = future))
        }
        assertTrue(pluginFailure.message!!.contains("plugin API: host 1.0.0"))
        val ideFailure = assertThrows(PluginCompatibilityException::class.java) {
            PluginCompatibility.requireCompatible(PluginApiRequirements(ideApi = future))
        }
        assertTrue(ideFailure.message!!.contains("IDE API: host 1.0.0"))
        PluginCompatibility.requireCompatible(PluginApiRequirements(
            HostApiRange("1.0.0", "1.0.1"), HostApiRange("0.0.0", "2.0.0")
        ))
    }
}
