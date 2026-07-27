package org.cosmicide.project

import org.cosmicide.editor.LspServerDefinition
import org.cosmicide.editor.LspServerRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ProjectExtensionContractsTest {
    @Test
    fun `choice fields require usable options`() {
        assertFails<IllegalArgumentException> {
            PluginFormField("runtime", "Runtime", PluginFormFieldType.CHOICE)
        }
        assertFails<IllegalArgumentException> { PluginFormOption("", "Default") }
        assertFails<IllegalArgumentException> { PluginFormOption("default", " ") }

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
        assertFails<IllegalArgumentException> { PluginFormField("", "Name") }
        assertFails<IllegalArgumentException> { PluginFormField("name", " ") }
    }

    @Test
    fun `operation progress accepts inclusive normalized bounds`() {
        assertEquals(0f, OperationUpdate("start", 0f).progress)
        assertEquals(1f, OperationUpdate("done", 1f).progress)
        assertEquals(null, OperationUpdate("working").progress)
        listOf(-0.01f, 1.01f, Float.POSITIVE_INFINITY).forEach { progress ->
            assertFails<IllegalArgumentException> { OperationUpdate("bad", progress) }
        }
    }

    @Test
    fun `terminal actions and project commands reject incomplete commands`() {
        assertFails<IllegalArgumentException> { TerminalAction("", "Install", "pacman -S git") }
        assertFails<IllegalArgumentException> { TerminalAction("git", "", "pacman -S git") }
        assertFails<IllegalArgumentException> { TerminalAction("git", "Install", " ") }
        assertFails<IllegalArgumentException> { ProjectCommand("sync", "Sync", "") }

        val command = ProjectCommand("sync", "Sync", "./sync", kind = ProjectCommandKind.SYNC)
        assertEquals(ProjectCommandKind.SYNC, command.kind)
    }

    @Test
    fun `command result success follows exit code only`() {
        assertTrue(CommandResult(0, "warnings").successful)
        assertFalse(CommandResult(1, "").successful)
        assertFalse(CommandResult(-1, "failed to launch").successful)
    }

    @Test
    fun `command request and project actions validate required identifiers`() {
        assertFails<IllegalArgumentException> { CommandRequest(" ", workingDirectory = File(".")) }
        assertFails<IllegalArgumentException> { ProjectAction("", "Build") }
        assertFails<IllegalArgumentException> { ProjectAction("build", " ") }
    }

    @Test
    fun `lsp request exposes file extension and definition validates startup fields`() {
        val request = LspServerRequest(
            Project(File("/tmp/example"), Language.Kotlin),
            File("/tmp/example/src/Main.kt")
        )
        assertEquals("kt", request.extension)

        val factory = org.cosmicide.editor.LspServerConnectionFactory {
            throw UnsupportedOperationException("not started in contract test")
        }
        assertFails<IllegalArgumentException> {
            LspServerDefinition("", setOf("kt"), "Kotlin", factory)
        }
        assertFails<IllegalArgumentException> {
            LspServerDefinition("kotlin", emptySet(), "Kotlin", factory)
        }
        assertFails<IllegalArgumentException> {
            LspServerDefinition("kotlin", setOf(""), "Kotlin", factory)
        }
        assertFails<IllegalArgumentException> {
            LspServerDefinition("kotlin", setOf("kt"), "", factory)
        }
        assertFails<IllegalArgumentException> {
            LspServerDefinition(
                "kotlin",
                setOf("kt", "kts"),
                "Kotlin",
                factory,
                initializationTimeoutMillis = 0
            )
        }
    }
}

private inline fun <reified T : Throwable> assertFails(block: () -> Unit): T {
    try {
        block()
    } catch (error: Throwable) {
        if (error is T) return error
        throw AssertionError("Expected ${T::class.java.name}, got ${error::class.java.name}", error)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
