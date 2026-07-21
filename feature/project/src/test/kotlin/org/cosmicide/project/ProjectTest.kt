package org.cosmicide.project

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class ProjectTest {
    @Test
    fun `derives stable project directories from root`() = withTempDirectory { root ->
        val project = Project(root, Language.Kotlin)

        assertEquals(root.name, project.name)
        assertEquals(root.resolve("src/main/kotlin"), project.srcDir)
        assertEquals(root.resolve("build"), project.buildDir)
        assertEquals(root.resolve("build/cache"), project.cacheDir)
        assertEquals(root.resolve("build/bin"), project.binDir)
        assertEquals(root.resolve("libs"), project.libDir)
    }

    @Test
    fun `application source set takes precedence when present`() = withTempDirectory { root ->
        root.resolve("src/main/java").mkdirs()
        root.resolve("app/src/main/java").mkdirs()

        assertEquals(
            root.resolve("app/src/main/java"),
            Project(root, Language.Java).srcDir
        )
    }

    @Test
    fun `arguments persist in project cache and reload from disk`() = withTempDirectory { root ->
        val project = Project(root, Language.Kotlin)
        project.args = listOf("hello world", "--flag=value")
        project.runtimeArgs = listOf("-Xmx512m", "-Dkey=value")

        val reloaded = Project(root, Language.Kotlin)

        assertEquals(listOf("hello world", "--flag=value"), reloaded.args)
        assertEquals(listOf("-Xmx512m", "-Dkey=value"), reloaded.runtimeArgs)
        assertEquals("hello world\n--flag=value", root.resolve("build/cache/args.txt").readText())
    }

    @Test
    fun `missing argument files produce empty lists without creating cache`() =
        withTempDirectory { root ->
            val project = Project(root, Language.Scala)

            assertTrue(project.args.isEmpty())
            assertTrue(project.runtimeArgs.isEmpty())
            assertFalse(project.cacheDir.exists())
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
