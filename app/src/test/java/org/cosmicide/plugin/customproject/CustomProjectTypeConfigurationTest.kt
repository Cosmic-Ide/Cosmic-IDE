package org.cosmicide.plugin.customproject

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CustomProjectTypeConfigurationTest {
    @Test
    fun normalizesProjectTypeAndCommands() {
        val normalized = CustomProjectTypeConfiguration(
            name = "  Cargo  ",
            markerFiles = listOf(" Cargo.toml ", "Cargo.toml", ""),
            syncCommand = " cargo fetch ",
            runCommand = " cargo run ",
            commands = listOf(
                CustomProjectCommandConfiguration(
                    name = " Test ",
                    command = " cargo test "
                )
            )
        ).normalized()

        assertEquals("Cargo", normalized.name)
        assertEquals(listOf("Cargo.toml"), normalized.markerFiles)
        assertEquals("cargo fetch", normalized.syncCommand)
        assertEquals("cargo run", normalized.runCommand)
        assertEquals("Test", normalized.commands.single().name)
    }

    @Test
    fun rejectsMarkerTraversal() {
        val configuration = CustomProjectTypeConfiguration(
            name = "Unsafe",
            markerFiles = listOf("../outside")
        )

        assertThrows(IllegalArgumentException::class.java) {
            configuration.validate()
        }
    }
}
