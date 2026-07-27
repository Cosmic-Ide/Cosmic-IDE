package org.cosmicide.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class EditorToolWindowSessionStateTest {
    @Test
    fun `opening the same Gradle task reruns its existing tab`() {
        val first = EditorToolWindowSessionState().openGradleTask("build")
        val rerun = first
            .updateBuildStatus(first.buildSessions.single().id, "Succeeded")
            .openGradleTask("build")

        assertEquals(1, rerun.buildSessions.size)
        assertEquals(1, rerun.buildSessions.single().runId)
        assertEquals("Running", rerun.buildSessions.single().status)
        assertEquals(first.selectedTabId, rerun.selectedTabId)
    }

    @Test
    fun `terminal commands always receive independent tabs`() {
        val state = EditorToolWindowSessionState()
            .openTerminal("Terminal", "bash", listOf("-i"))
            .openTerminal("Tests", "bash", listOf("-lc", "make test"))

        assertEquals(listOf(1, 2), state.buildSessions.map { it.id })
        assertEquals("build:2", state.selectedTabId)
    }

    @Test
    fun `closing selected build chooses the last remaining tab`() {
        val state = EditorToolWindowSessionState()
            .openGradleTask("build")
            .openGradleTask("test")
            .closeBuild(2)

        assertEquals("build:1", state.selectedTabId)
        assertEquals(listOf(1), state.buildSessions.map { it.id })
    }

    @Test
    fun `closing final build returns to sync tab`() {
        val state = EditorToolWindowSessionState()
            .openGradleTask("build")
            .closeBuild(1)

        assertEquals(SyncToolWindowTabId, state.selectedTabId)
        assertEquals(emptyList<EditorBuildSession>(), state.buildSessions)
    }

    @Test
    fun `rerunning project sync selects sync and increments run id`() {
        val state = EditorToolWindowSessionState()
            .openGradleTask("build")
            .updateProjectSyncStatus("Failed")
            .rerunProjectSync()

        assertEquals(SyncToolWindowTabId, state.selectedTabId)
        assertEquals(1, state.projectSyncRunId)
        assertEquals("Running", state.projectSyncStatus)
    }

    @Test
    fun `stopping sync and build updates their running status`() {
        val state = EditorToolWindowSessionState()
            .openGradleTask("build")
            .stopProjectSync()
            .stopBuild(1)

        assertEquals("Stopping", state.projectSyncStatus)
        assertEquals("Stopping", state.buildSessions.single().status)
    }
}
