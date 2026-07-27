package org.cosmicide.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.io.path.createTempDirectory

class EditorViewModelTest {

    @Test
    fun `saving active document does not overwrite previously opened file`() {
        withTempDir { dir ->
            val first = dir.resolve("First.kt").apply { writeText("first") }
            val second = dir.resolve("Second.kt").apply { writeText("second") }
            val viewModel = EditorViewModel()

            viewModel.openFile(first)
            viewModel.ensureDocument(first, first.readText())
            viewModel.onActiveContentChanged("first edited")

            viewModel.openFile(second, "first edited")
            viewModel.ensureDocument(second, second.readText())
            viewModel.onActiveContentChanged("second edited")

            assertEquals("first edited", first.readText())
            assertEquals("second edited", second.readText())
        }
    }

    @Test
    fun `switching tabs restores cached document content`() {
        withTempDir { dir ->
            val first = dir.resolve("First.kt").apply { writeText("first") }
            val second = dir.resolve("Second.kt").apply { writeText("second") }
            val viewModel = EditorViewModel()

            viewModel.openFile(first)
            viewModel.ensureDocument(first, first.readText())
            viewModel.onActiveContentChanged("first edited")
            assertFalse(viewModel.documentFor(first)?.isDirty ?: true)

            viewModel.openFile(second, "first edited")
            viewModel.ensureDocument(second, second.readText())
            viewModel.onActiveContentChanged("second edited")

            viewModel.openFile(first, "second edited")

            assertEquals("first edited", viewModel.cachedContent(first))
            assertEquals(first, viewModel.activeFile)
            assertTrue(first in viewModel.openFiles)
            assertTrue(second in viewModel.openFiles)
        }
    }

    @Test
    fun `preview images are never overwritten with editor text`() {
        withTempDir { dir ->
            val image = dir.resolve("preview.PNG").apply {
                writeBytes(byteArrayOf(1, 2, 3, 4))
            }
            val text = dir.resolve("notes.md").apply { writeText("# Notes") }
            val viewModel = EditorViewModel()

            viewModel.openFile(image)
            viewModel.openFile(text, currentEditorContent = null)

            assertEquals(listOf<Byte>(1, 2, 3, 4), image.readBytes().toList())
        }
    }

    private fun withTempDir(block: (File) -> Unit) {
        val dir = createTempDirectory("cosmic-editor-test").toFile()
        try {
            block(dir)
        } finally {
            dir.deleteRecursively()
        }
    }
}
