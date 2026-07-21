package org.cosmicide.util

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipUtilTest {
    @Test
    fun `compress and extract preserve nested file contents`() = withTempDirectory { root ->
        val source = root.resolve("source").apply { mkdirs() }
        source.resolve("README.md").writeText("Cosmic")
        source.resolve("src/main").mkdirs()
        source.resolve("src/main/Main.kt").writeText("fun main() = Unit")

        val archive = ByteArrayOutputStream().also(source::compressToZip).toByteArray()
        val target = root.resolve("target")
        ByteArrayInputStream(archive).unzip(target)

        assertEquals("Cosmic", target.resolve("README.md").readText())
        assertEquals("fun main() = Unit", target.resolve("src/main/Main.kt").readText())
        assertEquals(
            listOf("README.md", "src/main/Main.kt"),
            zipEntryNames(archive).sorted()
        )
    }

    @Test
    fun `extract rejects traversal-shaped entries without writing outside target`() =
        withTempDirectory { root ->
            val archive = zipOf(
                "../outside.txt" to "escape",
                "safe/../../outside.txt" to "escape",
                "safe/file.txt" to "inside"
            )
            val target = root.resolve("target")

            ByteArrayInputStream(archive).unzip(target)

            assertFalse(root.resolve("outside.txt").exists())
            assertEquals("inside", target.resolve("safe/file.txt").readText())
        }

    @Test
    fun `strip prefix extracts only matching subtree`() = withTempDirectory { root ->
        val archive = zipOf(
            "repo-main/src/Main.kt" to "main",
            "repo-main/README.md" to "readme",
            "other/file.txt" to "ignored"
        )

        ByteArrayInputStream(archive).unzip(root.resolve("target"), stripPrefix = "/repo-main/")

        assertEquals("main", root.resolve("target/src/Main.kt").readText())
        assertEquals("readme", root.resolve("target/README.md").readText())
        assertFalse(root.resolve("target/other/file.txt").exists())
    }

    @Test
    fun `skip existing preserves destination while extracting new files`() =
        withTempDirectory { root ->
            val target = root.resolve("target").apply { mkdirs() }
            target.resolve("existing.txt").writeText("local")
            val archive = zipOf(
                "existing.txt" to "archive",
                "new.txt" to "new"
            )

            ByteArrayInputStream(archive).unzip(target, skipExisting = true)

            assertEquals("local", target.resolve("existing.txt").readText())
            assertEquals("new", target.resolve("new.txt").readText())
        }

    @Test
    fun `stream extraction concatenates file entries and ignores directories`() {
        val archive = zipOf("first.txt" to "one", "folder/second.txt" to "two")
        val output = ByteArrayOutputStream()

        ByteArrayInputStream(archive).unzip(output)

        assertArrayEquals("onetwo".toByteArray(), output.toByteArray())
    }

    private fun zipOf(vararg entries: Pair<String, String>): ByteArray =
        ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                entries.forEach { (name, value) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(value.toByteArray())
                    zip.closeEntry()
                }
            }
        }.toByteArray()

    private fun zipEntryNames(archive: ByteArray): List<String> = buildList {
        ZipInputStream(ByteArrayInputStream(archive)).use { input ->
            while (true) {
                val entry = input.nextEntry ?: break
                add(entry.name)
                input.closeEntry()
            }
        }
    }

    private fun withTempDirectory(block: (File) -> Unit) {
        val root = Files.createTempDirectory("cosmic-zip-test").toFile()
        try {
            block(root)
        } finally {
            root.deleteRecursively()
        }
    }
}
