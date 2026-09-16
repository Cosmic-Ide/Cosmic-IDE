package org.cosmicide.project

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProjectExtensionContractsTest {
    @Test
    fun `choice fields require usable options`() {
        assertThrowsException<IllegalArgumentException> {
            PluginFormField("runtime", "Runtime", PluginFormFieldType.CHOICE)
        }
        assertThrowsException<IllegalArgumentException> { PluginFormOption("", "Default") }
        assertThrowsException<IllegalArgumentException> { PluginFormOption("default", " ") }

        val field = PluginFormField(
            id = "runtime",
            label = "Runtime",
            type = PluginFormFieldType.CHOICE,
            options = listOf(PluginFormOption("jdk", "JDK"))
        )
        assertEquals("jdk", field.options.single().value)
    }

    @Test
    fun `form fields reject blank identity and label`() {
        assertThrowsException<IllegalArgumentException> { PluginFormField("", "Name") }
        assertThrowsException<IllegalArgumentException> { PluginFormField("name", " ") }
    }

    @Test
    fun `form fields default to visible but can be hidden`() {
        assertTrue(PluginFormField("name", "Name").visible)
        assertFalse(PluginFormField("name", "Name", visible = false).visible)
    }

    @Test
    fun `form fields accept conditional visibility mappings`() {
        val field = PluginFormField(
            id = "command",
            label = "Create command",
            visibleWhen = mapOf("template" to "custom")
        )
        assertEquals("custom", field.visibleWhen.getValue("template"))
        assertThrowsException<IllegalArgumentException> {
            PluginFormField(
                id = "command",
                label = "Create command",
                visibleWhen = mapOf("" to "custom")
            )
        }
    }

    @Test
    fun `operation progress accepts inclusive normalized bounds`() {
        assertEquals(0f, OperationUpdate("start", 0f).progress)
        assertEquals(1f, OperationUpdate("done", 1f).progress)
        assertEquals(null, OperationUpdate("working").progress)
        listOf(-0.01f, 1.01f, Float.POSITIVE_INFINITY).forEach { progress ->
            assertThrowsException<IllegalArgumentException> { OperationUpdate("bad", progress) }
        }
    }

    @Test
    fun `project commands reject incomplete commands`() {
        assertThrowsException<IllegalArgumentException> { ProjectCommand("sync", "Sync", "") }

        val command = ProjectCommand("sync", "Sync", "./sync", kind = ProjectCommandKind.SYNC)
        assertEquals(ProjectCommandKind.SYNC, command.kind)
    }

    @Test
    fun `project command groups contain child commands instead of shell text`() {
        val build = ProjectCommand("build.debug", "Debug", "./gradlew assembleDebug")
        val group = ProjectCommand("build", "Build", children = listOf(build))

        assertEquals(build, group.children.single())
        assertThrowsException<IllegalArgumentException> {
            ProjectCommand("empty", "Empty", children = emptyList())
        }
        assertThrowsException<IllegalArgumentException> {
            ProjectCommand("ambiguous", "Ambiguous", "make", children = listOf(build))
        }
        assertThrowsException<IllegalArgumentException> {
            ProjectCommand(
                "sync",
                "Sync",
                kind = ProjectCommandKind.SYNC,
                children = listOf(build)
            )
        }
    }

    @Test
    fun `project tasks validate identity label and command`() {
        val task = ProjectTask(
            id = "maven.test",
            label = "Test",
            command = "mvn test",
            group = "Lifecycle"
        )
        assertEquals("Lifecycle", task.group)
        assertThrowsException<IllegalArgumentException> { ProjectTask("", "Test", "mvn test") }
        assertThrowsException<IllegalArgumentException> { ProjectTask("test", " ", "mvn test") }
        assertThrowsException<IllegalArgumentException> { ProjectTask("test", "Test", " ") }
    }

    @Test
    fun `command result success follows exit code only`() {
        assertTrue(CommandResult(0, "warnings").successful)
        assertFalse(CommandResult(1, "").successful)
        assertFalse(CommandResult(-1, "failed to launch").successful)
    }

    @Test
    fun `command request and project actions validate required identifiers`() {
        assertThrowsException<IllegalArgumentException> {
            CommandRequest(
                " ",
                workingDirectory = File(".")
            )
        }
        assertThrowsException<IllegalArgumentException> { ProjectAction("", "Build") }
        assertThrowsException<IllegalArgumentException> { ProjectAction("build", " ") }
    }
}

private inline fun <reified T : Throwable> assertThrowsException(block: () -> Unit): T {
    try {
        block()
    } catch (error: Throwable) {
        if (error is T) return error
        throw AssertionError("Expected ${T::class.java.name}, got ${error::class.java.name}", error)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
