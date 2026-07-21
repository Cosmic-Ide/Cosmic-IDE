package org.cosmicide.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class FileUtilTest {
    @Test
    fun `initialization creates each persistent data root`() {
        val root = Files.createTempDirectory("cosmic-file-util").toFile()
        try {
            FileUtil.init(root)

            assertTrue(FileUtil.isInitialized)
            assertEquals(root.resolve("projects"), FileUtil.projectDir)
            assertEquals(root.resolve("classpath"), FileUtil.classpathDir)
            assertEquals(root.resolve("plugins"), FileUtil.pluginDir)
            assertTrue(FileUtil.projectDir.isDirectory)
            assertTrue(FileUtil.classpathDir.isDirectory)
            assertTrue(FileUtil.pluginDir.isDirectory)
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `jdk permission repair reports missing installations`() {
        val root = Files.createTempDirectory("cosmic-jdk-missing").toFile()
        try {
            assertFalse(repairJdkExecutablePermissions(root.resolve("missing")))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test
    fun `jdk permission repair makes launchers executable`() {
        val root = Files.createTempDirectory("cosmic-jdk-permissions").toFile()
        try {
            val jdk = root.resolve("jdk").apply { mkdirs() }
            val java = jdk.resolve("bin/java").apply {
                parentFile?.mkdirs()
                writeText("#!/bin/sh\n")
                setExecutable(false, false)
            }
            jdk.resolve("bin/javac").apply { writeText("compiler"); setExecutable(false, false) }
            jdk.resolve("lib/jexec").apply { parentFile?.mkdirs(); writeText("exec") }

            assertTrue(repairJdkExecutablePermissions(jdk))
            assertTrue(java.canExecute())
            assertTrue(jdk.resolve("bin/javac").canExecute())
            assertTrue(jdk.resolve("lib/jexec").canExecute())
        } finally {
            root.deleteRecursively()
        }
    }
}
