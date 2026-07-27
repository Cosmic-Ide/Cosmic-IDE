package org.cosmicide.tooling

import org.gradle.tooling.GradleConnectionException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class OperationStateTest {
    @Test
    fun `wire params include every remotely configurable operation setting`() {
        val state = OperationState().apply {
            arguments += listOf("--stacktrace", "--offline")
            jvmArguments += listOf("-Xmx1g")
            systemProperties["mode"] = "test"
            environmentVariables["CI"] = "true"
            javaHome = File("/jdk")
            colorOutput = true
            detailedFailure = true
        }

        val params = state.toParams()

        assertEquals(
            listOf("--stacktrace", "--offline"),
            params["arguments"].asJsonArray.map { it.asString })
        assertEquals(listOf("-Xmx1g"), params["jvmArguments"].asJsonArray.map { it.asString })
        assertEquals("test", params["systemProperties"].asJsonObject["mode"].asString)
        assertEquals("true", params["env"].asJsonObject["CI"].asString)
        assertEquals(File("/jdk").absolutePath, params["javaHome"].asString)
        assertTrue(params["colorOutput"].asBoolean)
        assertTrue(params["detailedFailure"].asBoolean)
    }

    @Test
    fun `empty state emits explicit boolean defaults only`() {
        val params = OperationState().toParams()

        assertEquals(setOf("colorOutput", "detailedFailure"), params.keySet())
        assertFalse(params["colorOutput"].asBoolean)
        assertFalse(params["detailedFailure"].asBoolean)
    }

    @Test
    fun `operation ids are prefixed unique UUIDs`() {
        val ids = List(100) { newOpId() }

        assertEquals(100, ids.toSet().size)
        assertTrue(ids.all { it.startsWith("op-") && it.length > 20 })
    }

    @Test
    fun `connection exception wrapper preserves existing tooling failures`() {
        val existing = GradleConnectionException("existing")
        assertSame(existing, wrapAsConnectionException(existing))

        val cause = IllegalStateException("broken")
        val wrapped = wrapAsConnectionException(cause)
        assertEquals("broken", wrapped.message)
        assertSame(cause, wrapped.cause)
    }

    @Test
    fun `void result handler accepts Gradle null completion`() {
        var completed = false
        val handler = voidResultHandler(
            onComplete = { completed = true },
            onFailure = { throw AssertionError(it) }
        )

        handler.onComplete(null)

        assertTrue(completed)
    }
}
