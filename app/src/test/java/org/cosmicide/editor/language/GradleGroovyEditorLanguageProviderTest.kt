package org.cosmicide.editor.language

import org.cosmicide.editor.LspServerRequest
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class GradleGroovyEditorLanguageProviderTest {
    private val project = Project(File("/project"), Language.Kotlin)

    @Test
    fun `supports Gradle and Groovy files`() {
        assertTrue(providerSupports("build.gradle"))
        assertTrue(providerSupports("Script.GROOVY"))
        assertFalse(providerSupports("build.gradle.kts"))
        assertFalse(providerSupports("Main.java"))
    }

    @Test
    fun `passes Gradle settings in initialization options`() {
        val definition = GradleGroovyEditorLanguageProvider.createDefinition(
            request("build.gradle")
        )
        val initializationOptions = definition.initializationOptions as Map<*, *>
        val settings = initializationOptions["settings"] as Map<*, *>

        assertEquals(true, settings["gradleWrapperEnabled"])
        assertTrue(settings.containsKey("gradleHome"))
        assertTrue(settings.containsKey("gradleVersion"))
        assertTrue(settings.containsKey("gradleUserHome"))
        assertEquals(settings, definition.configuration)
        assertTrue(definition.traceIncomingMessages)
    }

    private fun providerSupports(name: String): Boolean {
        return GradleGroovyEditorLanguageProvider.supports(request(name))
    }

    private fun request(name: String) = LspServerRequest(
        project = project,
        file = File(project.root, name)
    )
}
