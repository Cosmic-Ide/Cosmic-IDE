package org.cosmicide.model

import org.cosmicide.project.Language
import org.cosmicide.project.Project
import org.cosmicide.project.ProjectTypeProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ProjectRepositoryTest {
    @Test
    fun `discovers project language from root and app source layouts`() = withProjects { root ->
        root.resolve("java/src/main/java").mkdirs()
        root.resolve("kotlin/app/src/main/kotlin").mkdirs()
        root.resolve("scala/src/main/scala").mkdirs()

        val projects = FileSystemProjectRepository(root).projects().associateBy(Project::name)

        assertEquals(Language.Java, projects.getValue("java").language)
        assertEquals(Language.Kotlin, projects.getValue("kotlin").language)
        assertEquals(Language.Scala, projects.getValue("scala").language)
    }

    @Test
    fun `deletes only direct child projects`() = withProjects { root ->
        val projectRoot = root.resolve("sample").apply { mkdirs() }
        val repository = FileSystemProjectRepository(root)

        repository.delete(Project(projectRoot, Language.Kotlin))

        assertFalse(projectRoot.exists())

        val outside = Files.createTempDirectory("outside-project").toFile()
        try {
            assertThrows(IllegalArgumentException::class.java) {
                repository.delete(Project(outside, Language.Kotlin))
            }
        } finally {
            outside.deleteRecursively()
        }
    }

    @Test
    fun `installed project type provider participates in discovery`() = withProjects { root ->
        root.resolve("rust/Cargo.toml").apply {
            checkNotNull(parentFile).mkdirs()
            writeText("[package]\nname = \"rust\"")
        }
        val rustProvider = object : ProjectTypeProvider {
            override val id = "test.rust"
            override val languageName = "Rust"
            override val fileExtension = "rs"

            override fun supports(projectRoot: File): Boolean {
                return projectRoot.resolve("Cargo.toml").isFile
            }
        }

        val project = FileSystemProjectRepository(root) { listOf(rustProvider) }
            .projects()
            .single()

        assertEquals(Language.Custom("Rust", "rs"), project.language)
    }

    private fun withProjects(block: (File) -> Unit) {
        val root = Files.createTempDirectory("cosmic-projects").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
