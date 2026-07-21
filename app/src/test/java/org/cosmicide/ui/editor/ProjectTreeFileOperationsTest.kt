package org.cosmicide.ui.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectTreeFileOperationsTest {
    @Test
    fun `creates renames and deletes entries inside project`() = withProject { root, operations ->
        val source = operations.create(root, "src", directory = true)
        val created = operations.create(source, "Main", suffix = ".kt")
        val renamed = operations.rename(created, "App.kt")

        assertEquals("App.kt", renamed.name)
        assertTrue(renamed.isFile)

        operations.delete(renamed)
        assertFalse(renamed.exists())
    }

    @Test
    fun `rejects path traversal names`() = withProject { root, operations ->
        assertThrows(IllegalArgumentException::class.java) {
            operations.create(root, "../outside")
        }
        assertThrows(IllegalArgumentException::class.java) {
            operations.create(root, "nested/file")
        }
    }

    @Test
    fun `cannot rename or delete project root`() = withProject { root, operations ->
        assertThrows(IllegalArgumentException::class.java) {
            operations.rename(root, "renamed")
        }
        assertThrows(IllegalArgumentException::class.java) {
            operations.delete(root)
        }
    }

    @Test
    fun `cannot mutate a file outside project`() = withProject { _, operations ->
        val outside = File.createTempFile("cosmic-outside", ".txt")
        try {
            assertThrows(IllegalArgumentException::class.java) {
                operations.delete(outside)
            }
        } finally {
            outside.delete()
        }
    }

    private fun withProject(block: (File, ProjectTreeFileOperations) -> Unit) {
        val root = Files.createTempDirectory("cosmic-project").toFile()
        try {
            block(root, ProjectTreeFileOperations(root))
        } finally {
            root.deleteRecursively()
        }
    }
}
