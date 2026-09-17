/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.plugin.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

/**
 * Regression gate for the versioned/bounded plugin manifest validation, exercised only through
 * the legacy [PluginManifestReader.read] entry point on real plugin directories containing an
 * actual plugin.json file. JSON bodies are hardcoded and no new metadata types or constants are
 * referenced, so this test stays ABI-compatible with the original reader signature and survives
 * before/after refactor proofs.
 */
class PluginManifestLoadingGateTest {

    @Test
    fun `legacy read gate accepts supported manifests and rejects oversized or unsupported ones`() {
        val root = Files.createTempDirectory("cosmic-plugin-loading-gate").toFile()
        try {
            val manifest = root.resolve("plugin.json")
            val validLegacyManifest =
                """{"id":"org.example.gate","name":"Gate","entryClass":"org.example.GatePlugin","classPath":["main.jar"]}"""

            // 1. Valid legacy (unversioned) manifest reads through the legacy entry point.
            manifest.writeText(validLegacyManifest)
            val descriptor = PluginManifestReader.read(root)
            assertNotNull(descriptor)
            assertEquals("org.example.gate", descriptor!!.id)
            assertEquals("Gate", descriptor.name)
            assertEquals("org.example.GatePlugin", descriptor.entryClass)
            assertEquals(listOf("main.jar"), descriptor.classPath)

            // 2. schemaVersion 1 reads identically.
            manifest.writeText(validLegacyManifest.removeSuffix("}") + ",\"schemaVersion\":1}")
            assertEquals("org.example.gate", PluginManifestReader.read(root)?.id)

            // 3. schemaVersion 2 is rejected with the expected reason.
            manifest.writeText(validLegacyManifest.removeSuffix("}") + ",\"schemaVersion\":2}")
            val versionFailure = runCatching { PluginManifestReader.read(root) }.exceptionOrNull()
            assertTrue("Expected schemaVersion rejection, got $versionFailure", versionFailure is IllegalArgumentException)
            assertEquals("schemaVersion must be the supported integer 1", versionFailure?.message)

            // 4. A valid legacy manifest padded past the 65536 UTF-8 byte budget is rejected
            //    with the expected size reason, even though the JSON itself would parse.
            val paddedManifest = validLegacyManifest.padEnd(65537, ' ')
            assertTrue(paddedManifest.toByteArray(Charsets.UTF_8).size > 65536)
            manifest.writeText(paddedManifest)
            val sizeFailure = runCatching { PluginManifestReader.read(root) }.exceptionOrNull()
            assertTrue("Expected size rejection, got $sizeFailure", sizeFailure is IllegalArgumentException)
            assertEquals("Manifest exceeds 65536 bytes", sizeFailure?.message)

            // 5. Rewriting the valid manifest afterward restores acceptance; the earlier
            //    rejections came from the payload, not a wedged loader.
            manifest.writeText(validLegacyManifest)
            assertEquals("org.example.gate", PluginManifestReader.read(root)?.id)
        } finally {
            root.deleteRecursively()
        }
    }
}
