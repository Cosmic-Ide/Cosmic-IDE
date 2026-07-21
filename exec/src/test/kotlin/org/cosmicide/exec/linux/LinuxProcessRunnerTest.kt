package org.cosmicide.exec.linux

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class LinuxProcessRunnerTest {
    @Test
    fun `command parser handles whitespace quotes escapes and adjacent fragments`() {
        assertEquals(
            listOf("git", "commit", "-m", "hello world", "it's fine", "plain value"),
            LinuxProcessRunner.parseCommandLine(
                "  git commit -m \"hello world\" 'it\\'s fine' plain\\ value  "
            )
        )
        assertEquals(
            listOf("prefix-middle-suffix"),
            LinuxProcessRunner.parseCommandLine("prefix-\"middle\"-'suffix'")
        )
    }

    @Test
    fun `command parser preserves explicitly empty arguments`() {
        assertEquals(
            listOf("command", "", "", "end"),
            LinuxProcessRunner.parseCommandLine("command '' \"\" end")
        )
    }

    @Test
    fun `command parser preserves trailing escaped slash`() {
        assertEquals(listOf("path\\"), LinuxProcessRunner.parseCommandLine("path\\"))
    }

    @Test
    fun `command parser rejects unclosed quotes`() {
        val error = assertFails<IllegalArgumentException> {
            LinuxProcessRunner.parseCommandLine("command 'unfinished")
        }
        assertEquals("Unclosed quote in command", error.message)
    }

    @Test
    fun `absolute executable is returned without requiring existence`() {
        val absolute = File("/does/not/need/to/exist")

        assertEquals(
            absolute,
            LinuxProcessRunner.resolveExecutable(absolute.path, File("."), emptyList())
        )
    }

    @Test
    fun `relative executable path is resolved canonically against working directory`() =
        withTempDirectory { root ->
            val expected = root.resolve("tools/run").canonicalFile
            assertEquals(
                expected,
                LinuxProcessRunner.resolveExecutable("./tools/../tools/run", root, emptyList())
            )
        }

    @Test
    fun `path lookup selects first matching regular file`() = withTempDirectory { root ->
        val first = root.resolve("first").apply { mkdirs() }
        val second = root.resolve("second").apply { mkdirs() }
        first.resolve("tool").mkdir()
        val expected = second.resolve("tool").apply { writeText("binary") }

        assertEquals(
            expected,
            LinuxProcessRunner.resolveExecutable("tool", root, listOf(first, second))
        )
    }

    @Test
    fun `path lookup reports missing command`() = withTempDirectory { root ->
        val error = assertFails<IllegalArgumentException> {
            LinuxProcessRunner.resolveExecutable("definitely-not-cosmic-command", root, emptyList())
        }
        assertTrue(error.message.orEmpty().contains("Command not found"))
    }

    private fun withTempDirectory(block: (File) -> Unit) {
        val root = Files.createTempDirectory("cosmic-exec-test").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
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
