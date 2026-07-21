package org.cosmicide.editor.lsp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomLspConfigurationTest {
    @Test
    fun `normalizes extension script and grammar link`() {
        val configuration = configuration(
            id = "rust",
            extension = " .RS ",
            grammarLink = " https://example.com/rust.tmLanguage.json "
        ).copy(startScript = " rust-analyzer ").normalized()

        assertEquals("rs", configuration.fileExtension)
        assertEquals("rust-analyzer", configuration.startScript)
        assertEquals(
            "https://example.com/rust.tmLanguage.json",
            configuration.textMateGrammarLink
        )
    }

    @Test
    fun `rejects unsupported grammar link schemes`() {
        val configuration = configuration(
            id = "rust",
            extension = "rs",
            grammarLink = "ftp://example.com/rust.tmLanguage.json"
        )

        assertThrows(IllegalArgumentException::class.java) {
            configuration.validate()
        }
    }

    @Test
    fun `keeps only the first enabled configuration for each extension`() {
        val configurations = listOf(
            configuration("first-rust", "rs"),
            configuration("python", "py"),
            configuration("second-rust", "RS")
        ).map(CustomLspConfiguration::normalized)

        val resolved = configurations.enforceSingleActiveConfigurationPerExtension()

        assertTrue(resolved[0].enabled)
        assertTrue(resolved[1].enabled)
        assertFalse(resolved[2].enabled)
    }

    private fun configuration(
        id: String,
        extension: String,
        grammarLink: String? = null
    ) = CustomLspConfiguration(
        id = id,
        name = id,
        fileExtension = extension,
        startScript = "server",
        textMateGrammarLink = grammarLink
    )
}
