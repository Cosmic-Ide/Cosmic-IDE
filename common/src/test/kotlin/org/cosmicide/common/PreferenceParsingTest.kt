package org.cosmicide.common

import org.junit.Assert.assertEquals
import org.junit.Test

class PreferenceParsingTest {
    @Test
    fun `integer preference accepts whitespace and falls back for malformed values`() {
        assertEquals(21, parseIntPreference(" 21 ", 17))
        assertEquals(17, parseIntPreference(null, 17))
        assertEquals(17, parseIntPreference("twenty-one", 17))
        assertEquals(17, parseIntPreference("999999999999999999", 17))
    }

    @Test
    fun `float preference clamps finite values inclusively`() {
        assertEquals(0f, parseBoundedFloatPreference("-1", 0.5f, 0f, 1f))
        assertEquals(0.25f, parseBoundedFloatPreference(" .25 ", 0.5f, 0f, 1f))
        assertEquals(1f, parseBoundedFloatPreference("2", 0.5f, 0f, 1f))
    }

    @Test
    fun `float preference rejects malformed and non finite values`() {
        listOf(null, "", "invalid", "NaN", "Infinity", "-Infinity").forEach { value ->
            assertEquals(0.9f, parseBoundedFloatPreference(value, 0.9f, 0f, 1f))
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun `float bounds must be ordered`() {
        parseBoundedFloatPreference("1", 0f, minimum = 2f, maximum = 1f)
    }
}
