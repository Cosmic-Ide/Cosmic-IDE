package org.cosmicide.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HomeRepositoriesTest {
    @Test
    fun `derives project name from zip archive`() {
        assertEquals("cosmic.ide", projectNameFromArchiveName(" cosmic.ide.zip "))
        assertEquals("project", projectNameFromArchiveName("project"))
    }

    @Test
    fun `rejects empty and path-like archive names`() {
        assertThrows(IllegalArgumentException::class.java) {
            projectNameFromArchiveName(".zip")
        }
        assertThrows(IllegalArgumentException::class.java) {
            projectNameFromArchiveName("../outside.zip")
        }
    }
}
