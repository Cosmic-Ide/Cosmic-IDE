package org.cosmicide.ui.editor

import org.cosmicide.project.ProjectCommand
import org.cosmicide.project.ProjectCommandKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ProjectSyncStrategyTest {
    @Test
    fun `Gradle wrapper takes precedence over plugin sync commands`() {
        val command = syncCommand("plugin-sync")

        val strategy = resolveProjectSyncStrategy(
            hasGradleWrapper = true,
            projectCommands = listOf(command)
        )

        assertEquals(ProjectSyncStrategy.GradleWrapper, strategy)
    }

    @Test
    fun `first plugin sync command is selected without Gradle wrapper`() {
        val first = syncCommand("first")
        val second = syncCommand("second")

        val strategy = resolveProjectSyncStrategy(
            hasGradleWrapper = false,
            projectCommands = listOf(first, second)
        )

        assertSame(first, (strategy as ProjectSyncStrategy.PluginCommand).command)
    }

    @Test
    fun `non-sync commands do not create a sync strategy`() {
        val strategy = resolveProjectSyncStrategy(
            hasGradleWrapper = false,
            projectCommands = listOf(
                ProjectCommand("build", "Build", "make", kind = ProjectCommandKind.BUILD)
            )
        )

        assertEquals(ProjectSyncStrategy.Unavailable, strategy)
    }

    @Test
    fun `nested sync command is selected from command tree`() {
        val sync = syncCommand("nested-sync")
        val group = ProjectCommand(
            id = "maintenance",
            label = "Maintenance",
            children = listOf(sync)
        )

        assertEquals(
            ProjectSyncStrategy.PluginCommand(sync),
            resolveProjectSyncStrategy(
                hasGradleWrapper = false,
                projectCommands = listOf(group)
            )
        )
    }

    private fun syncCommand(id: String) = ProjectCommand(
        id = id,
        label = id,
        command = "echo $id",
        kind = ProjectCommandKind.SYNC
    )
}
