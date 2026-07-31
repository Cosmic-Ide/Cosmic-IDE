package org.cosmicide.ui.editor

import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectCommandKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProjectSyncStrategyTest {
    @Test
    fun `first plugin sync command is selected without Gradle wrapper`() {
        val first = syncCommand("first")
        val second = syncCommand("second")

        val strategy = resolveProjectSyncStrategy(
            projectCommands = listOf(first, second)
        )

        assertSame(first, (strategy as ProjectSyncStrategy.PluginCommand).command)
    }

    @Test
    fun `non-sync commands do not create a sync strategy`() {
        val strategy = resolveProjectSyncStrategy(
            projectCommands = listOf(
                ProjectCommand("build", "Build", "make", kind = ProjectCommandKind.BUILD)
            )
        )

        assertEquals(ProjectSyncStrategy.Unavailable, strategy)
    }

    private fun syncCommand(id: String) = ProjectCommand(
        id = id,
        label = id,
        command = "echo $id",
        kind = ProjectCommandKind.SYNC
    )
}
