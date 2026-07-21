package org.cosmicide.ui.settings.extensions

import org.cosmicide.editor.lsp.CustomLspConfiguration
import org.cosmicide.plugin.customproject.CustomProjectTypeConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class ExtensionSettingsFormsTest {
    @Test
    fun `LSP form preserves identity and enabled state while normalizing values`() {
        val existing = CustomLspConfiguration(
            id = "existing",
            name = "Old",
            fileExtension = "rs",
            startScript = "old",
            enabled = false
        )

        val configuration = buildCustomLspConfiguration(
            existing = existing,
            name = " Rust ",
            fileExtension = " .RS ",
            startScript = " rust-analyzer ",
            grammarLink = " https://example.com/rust.json "
        )

        assertEquals("existing", configuration.id)
        assertEquals("rs", configuration.fileExtension)
        assertEquals("rust-analyzer", configuration.startScript)
        assertFalse(configuration.enabled)
    }

    @Test
    fun `project type form parses labelled commands and marker lines`() {
        val configuration = buildCustomProjectTypeConfiguration(
            existing = null,
            name = " Cargo ",
            markers = "Cargo.toml\n.cargo/config.toml",
            createCommand = " cargo init . ",
            syncCommand = " cargo fetch ",
            buildCommand = " cargo build ",
            runCommand = " cargo run ",
            additionalCommands = "Test :: cargo test\nFormat :: cargo fmt",
            id = "cargo"
        )

        assertEquals(listOf("Cargo.toml", ".cargo/config.toml"), configuration.markerFiles)
        assertEquals(listOf("Test", "Format"), configuration.commands.map { it.name })
        assertEquals(listOf("cargo test", "cargo fmt"), configuration.commands.map { it.command })
    }

    @Test
    fun `project type form preserves existing disabled state`() {
        val existing = CustomProjectTypeConfiguration(
            id = "existing",
            name = "Existing",
            enabled = false
        )

        val configuration = buildCustomProjectTypeConfiguration(
            existing = existing,
            name = "Updated",
            markers = "marker",
            createCommand = "",
            syncCommand = "",
            buildCommand = "",
            runCommand = "",
            additionalCommands = ""
        )

        assertEquals("existing", configuration.id)
        assertFalse(configuration.enabled)
    }

    @Test
    fun `project type form rejects malformed additional commands`() {
        assertThrows(IllegalArgumentException::class.java) {
            buildCustomProjectTypeConfiguration(
                existing = null,
                name = "Cargo",
                markers = "Cargo.toml",
                createCommand = "",
                syncCommand = "",
                buildCommand = "",
                runCommand = "",
                additionalCommands = "cargo test",
                id = "cargo"
            )
        }
    }
}
