package org.cosmicide.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectCommandShellTest {
    @Test
    fun `project commands use the same interactive bash mode as terminal screen`() {
        val arguments = projectCommandShellArguments("scala-cli setup-ide .")

        assertEquals(listOf("-i", "-c", "scala-cli setup-ide ."), arguments)
    }
}
