package org.cosmicide.editor.preview

import org.cosmicide.editor.EditorExtensionPoints
import org.cosmicide.editor.EditorPreviewMatchRequest
import org.cosmicide.editor.EditorPreviewPresentation
import org.cosmicide.plugin.api.DefaultExtensionRegistry
import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class BuiltinPreviewProvidersTest {
    private val project = Project(File("/project"), Language.Empty)

    @Test
    fun `built in preview providers register through the extension point`() {
        val registry = DefaultExtensionRegistry()
        registerBuiltinPreviewExtensions(registry)

        assertEquals(
            setOf(
                "org.cosmicide.editor.preview.markdown",
                "org.cosmicide.editor.preview.html",
                "org.cosmicide.editor.preview.image"
            ),
            registry.extensions(EditorExtensionPoints.PREVIEW_PROVIDER).map { it.id }.toSet()
        )
    }

    @Test
    fun `images are preview only while svg remains code`() {
        assertTrue(ImagePreviewProvider.supports(request("photo.PNG")))
        assertFalse(ImagePreviewProvider.supports(request("icon.svg")))
        assertEquals(
            EditorPreviewPresentation.PREVIEW_ONLY,
            ImagePreviewProvider.presentation
        )
    }

    private fun request(name: String) = EditorPreviewMatchRequest(
        project = project,
        file = File(project.root, name)
    )
}
