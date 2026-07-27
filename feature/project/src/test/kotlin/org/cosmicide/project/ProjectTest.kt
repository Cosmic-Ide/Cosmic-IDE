package org.cosmicide.project

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.file.Files

class ProjectTest {
    @Test
    fun `derives stable project directories from root`() = withTempDirectory { root ->
        val project = Project(root, Language.Kotlin)

        assertEquals(root.name, project.name)
    }

    @Test
    fun `project serialization preserves root and language`() = withTempDirectory { root ->
        val original = Project(root.canonicalFile, Language.Scala)

        val encoded = Json.encodeToString(Project.serializer(), original)
        val decoded = Json.decodeFromString(Project.serializer(), encoded)

        assertEquals(original.root.absolutePath, decoded.root.absolutePath)
        assertEquals(Language.Scala::class, decoded.language::class)
        assertEquals(original.name, decoded.name)
    }

    private fun withTempDirectory(block: (java.io.File) -> Unit) {
        val root = Files.createTempDirectory("cosmic-project-model").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
