package org.cosmicide.ui.terminal

import org.junit.Assert.assertEquals
import org.junit.Test

class TerminalSupportTest {
    @Test
    fun `control modifier maps alphabetic keys case insensitively`() {
        ('a'..'z').forEachIndexed { index, char ->
            assertEquals(index + 1, char.code.toTerminalCodePoint(ctrlPressed = true))
            assertEquals(
                index + 1,
                char.uppercaseChar().code.toTerminalCodePoint(ctrlPressed = true)
            )
        }
    }

    @Test
    fun `control modifier maps terminal punctuation aliases`() {
        val expectations = mapOf(
            '[' to 27, '3' to 27,
            '\\' to 28, '4' to 28,
            ']' to 29, '5' to 29,
            '^' to 30, '6' to 30,
            '_' to 31, '7' to 31, '/' to 31,
            '8' to 127, '?' to 127
        )
        expectations.forEach { (key, expected) ->
            assertEquals(expected, key.code.toTerminalCodePoint(ctrlPressed = true))
        }
    }

    @Test
    fun `unmodified and unmapped keys pass through unchanged`() {
        assertEquals('a'.code, 'a'.code.toTerminalCodePoint(ctrlPressed = false))
        assertEquals('!'.code, '!'.code.toTerminalCodePoint(ctrlPressed = true))
        assertEquals(0x1f642, 0x1f642.toTerminalCodePoint(ctrlPressed = true))
    }

    @Test
    fun `control char helper converts letters and preserves other characters`() {
        assertEquals("\u0001", 'a'.controlChar())
        assertEquals("\u001a", 'Z'.controlChar())
        assertEquals("3", '3'.controlChar())
        assertEquals("?", '?'.controlChar())
    }
}
