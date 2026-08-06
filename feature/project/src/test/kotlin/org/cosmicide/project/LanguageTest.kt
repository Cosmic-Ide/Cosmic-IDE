package org.cosmicide.project

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class LanguageTest {

    @Test
    fun `language serializes and restores its subtype`() {
        listOf(
            Language.Custom("Rust", "rs")
        ).forEach { value ->
            val encoded = Json.encodeToString(Language.serializer(), value)
            val decoded = Json.decodeFromString(Language.serializer(), encoded)
            assertEquals(value, decoded)
            assertEquals(value.extension, decoded.extension)
            assertEquals(value.name, decoded.name)
        }
    }

    @Test
    fun `custom language validates plugin supplied metadata`() {
        assertFails<IllegalArgumentException> { Language.Custom("", "rs") }
        assertFails<IllegalArgumentException> { Language.Custom("Rust", ".rs") }
    }
}

internal inline fun <reified T : Throwable> assertFails(block: () -> Unit): T {
    try {
        block()
    } catch (error: Throwable) {
        if (error is T) return error
        throw AssertionError("Expected ${T::class.java.name}, got ${error::class.java.name}", error)
    }
    throw AssertionError("Expected ${T::class.java.name}")
}
