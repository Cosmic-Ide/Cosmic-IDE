package org.cosmicide.plugin.runtime.loading

import org.cosmicide.plugin.api.PluginDescriptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class PluginArtifactsTest {
    @Test
    fun `declared artifacts preserve manifest order and omit missing files`() =
        withPluginDirectory { root ->
            val second = root.resolve("second.dex").apply { writeText("dex") }
            val first = root.resolve("first.jar").apply { writeText("jar") }
            val descriptor = descriptor(
                classPath = listOf("second.dex", "missing.apk", "first.jar")
            )

            assertEquals(listOf(second, first), resolvePluginArtifacts(root, descriptor))
        }

    @Test
    fun `known conventional artifacts take precedence over directory scan`() =
        withPluginDirectory { root ->
            val apk = root.resolve("plugin.apk").apply { writeText("apk") }
            val jar = root.resolve("plugin.jar").apply { writeText("jar") }
            root.resolve("aaa.dex").writeText("dex")

            assertEquals(listOf(apk, jar), resolvePluginArtifacts(root, descriptor()))
        }

    @Test
    fun `fallback scan includes supported artifacts in deterministic order`() =
        withPluginDirectory { root ->
            val b = root.resolve("b.jar").apply { writeText("jar") }
            val a = root.resolve("a.dex").apply { writeText("dex") }
            root.resolve("notes.txt").writeText("ignore")
            root.resolve("nested.apk").mkdir()

            println(resolvePluginArtifacts(root, descriptor()))

            assertEquals(listOf(a, b), resolvePluginArtifacts(root, descriptor()))
        }

    @Test
    fun `empty directory resolves no artifacts`() = withPluginDirectory { root ->
        assertTrue(resolvePluginArtifacts(root, descriptor()).isEmpty())
    }

    private fun descriptor(classPath: List<String> = emptyList()) = PluginDescriptor(
        id = "org.example.plugin",
        name = "Plugin",
        version = "1",
        entryClass = "org.example.Plugin",
        classPath = classPath
    )

    private fun withPluginDirectory(block: (File) -> Unit) {
        val root = Files.createTempDirectory("cosmic-plugin-artifacts").toFile().canonicalFile
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
