package org.cosmicide.project

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class LanguageTest {
    @Test
    fun `resolves every supported source extension`() {
        assertSame(Language.Java, language("java"))
        assertSame(Language.Kotlin, language("kt"))
        assertSame(Language.Scala, language("scala"))
    }

    @Test
    fun `extension lookup is exact and rejects unsupported values`() {
        listOf("kotlin", ".kt", "KT", "", " kt ").forEach { extension ->
            assertFails<IllegalArgumentException> { language(extension) }
        }
    }

    @Test
    fun `language serializes and restores its subtype`() {
        listOf(Language.Java, Language.Kotlin, Language.Scala).forEach { value ->
            val encoded = Json.encodeToString(Language.serializer(), value)
            val decoded = Json.decodeFromString(Language.serializer(), encoded)
            assertEquals(value::class, decoded::class)
            assertEquals(value.extension, decoded.extension)
            assertEquals(value.name, decoded.name)
        }
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
