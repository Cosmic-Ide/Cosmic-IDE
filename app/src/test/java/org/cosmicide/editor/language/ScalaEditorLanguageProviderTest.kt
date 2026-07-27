package org.cosmicide.editor.language

import org.cosmicide.editor.LspServerRequest
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ScalaEditorLanguageProviderTest {
    private val project = Project(File("/project"), Language.Scala)

    @Test
    fun `supports every Metals file extension`() {
        listOf("Main.scala", "script.sc", "build.sbt", "build.mill").forEach {
            assertTrue(ScalaEditorLanguageProvider.supports(request(it)))
        }
        assertFalse(ScalaEditorLanguageProvider.supports(request("Main.java")))
    }

    @Test
    fun `links one Metals definition to all supported extensions`() {
        val definition = ScalaEditorLanguageProvider.createDefinition(request("build.mill"))

        assertEquals(setOf("scala", "sc", "sbt", "mill"), definition.fileExtensions)
    }

    private fun request(name: String) = LspServerRequest(
        project = project,
        file = File(project.root, name)
    )
}
