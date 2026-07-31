package org.cosmicide.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorToolWindowSessionStateTest {

    @Test
    fun `terminal commands always receive independent tabs`() {
        val state = EditorToolWindowSessionState()
            .openTerminal("Terminal", "bash", listOf("-i"))
            .openTerminal("Tests", "bash", listOf("-lc", "make test"))

        assertEquals(listOf(1, 2), state.buildSessions.map { it.id })
        assertEquals("build:2", state.selectedTabId)
    }
}
