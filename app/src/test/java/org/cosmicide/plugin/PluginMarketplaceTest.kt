package org.cosmicide.plugin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class PluginMarketplaceTest {
    @Test
    fun `parses checksum pinned plugin entry`() {
        val entries = parsePluginRepository(
            """
            [
              {
                "id": "org.example.rust",
                "name": "Rust",
                "version": "1.0.0",
                "downloadUrl": "https://example.com/rust.zip",
                "sha256": "${"ab".repeat(32)}"
              }
            ]
            """.trimIndent()
        )

        assertEquals("org.example.rust", entries.single().id)
    }

    @Test
    fun `parses separate short and markdown descriptions`() {
        val entry = parsePluginRepository(
            """
            [{
              "id": "org.example.rust",
              "name": "Rust",
              "version": "1.0.0",
              "description": "Rust language support",
              "detailedDescription": "# Rust\n\nFull **Markdown** details.",
              "downloadUrl": "https://example.com/rust.zip",
              "sha256": "${"ab".repeat(32)}"
            }]
            """.trimIndent()
        ).single()

        assertEquals("Rust language support", entry.shortDescription)
        assertEquals("# Rust\n\nFull **Markdown** details.", entry.detailedDescription)
    }

    @Test
    fun `uses legacy description as marketplace summary`() {
        val entry = parsePluginRepository(
            """
            [{
              "id": "org.example.legacy",
              "name": "Legacy",
              "version": "1.0.0",
              "description": "Works with older repository indexes.",
              "downloadUrl": "https://example.com/legacy.zip",
              "sha256": "${"ab".repeat(32)}"
            }]
            """.trimIndent()
        ).single()

        assertEquals(entry.detailedDescription, entry.shortDescription)
    }

    @Test
    fun `rejects unpinned or insecure plugin entry`() {
        assertThrows(IllegalArgumentException::class.java) {
            parsePluginRepository(
                """
                [{
                  "id": "org.example.rust",
                  "name": "Rust",
                  "version": "1",
                  "downloadUrl": "http://example.com/rust.zip",
                  "sha256": "missing"
                }]
                """.trimIndent()
            )
        }
    }

    @Test
    fun `rejects duplicate plugin ids`() {
        val entry =
            """
            {
              "id": "org.example.rust",
              "name": "Rust",
              "version": "1",
              "downloadUrl": "https://example.com/rust.zip",
              "sha256": "${"ab".repeat(32)}"
            }
            """.trimIndent()

        assertThrows(IllegalArgumentException::class.java) {
            parsePluginRepository("[$entry,$entry]")
        }
    }

    @Test
    fun `archive extraction rejects paths outside staging directory`() {
        withTemporaryDirectory { root ->
            val archive = root.resolve("plugin.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("../outside"))
                zip.write("bad".toByteArray())
                zip.closeEntry()
            }
            val staging = root.resolve("staging").apply { mkdirs() }

            assertThrows(IllegalArgumentException::class.java) {
                extractPluginArchive(archive, staging)
            }
        }
    }

    @Test
    fun `plugin artifacts are read only before loading`() {
        withTemporaryDirectory { root ->
            val artifact = root.resolve("plugin.apk").apply {
                writeText("dex")
            }

            makeArtifactsReadOnly(listOf(artifact))

            assertEquals(false, artifact.canWrite())
        }
    }

    private fun withTemporaryDirectory(block: (File) -> Unit) {
        val root = Files.createTempDirectory("cosmic-plugin-test").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
