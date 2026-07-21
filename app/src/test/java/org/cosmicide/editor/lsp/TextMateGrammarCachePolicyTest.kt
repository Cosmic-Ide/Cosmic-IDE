package org.cosmicide.editor.lsp

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class TextMateGrammarCachePolicyTest {
    @Test
    fun `cache is fresh through the seven day boundary`() {
        withCacheFile { cacheFile ->
            val now = 10L * TEXT_MATE_GRAMMAR_CACHE_MAX_AGE_MILLIS
            cacheFile.setLastModified(now - TEXT_MATE_GRAMMAR_CACHE_MAX_AGE_MILLIS)

            assertTrue(isTextMateGrammarCacheFresh(cacheFile, now))
        }
    }

    @Test
    fun `cache older than seven days is stale`() {
        withCacheFile { cacheFile ->
            val now = 10L * TEXT_MATE_GRAMMAR_CACHE_MAX_AGE_MILLIS
            cacheFile.setLastModified(now - TEXT_MATE_GRAMMAR_CACHE_MAX_AGE_MILLIS - 1)

            assertFalse(isTextMateGrammarCacheFresh(cacheFile, now))
        }
    }

    @Test
    fun `missing and future-dated cache entries are not fresh`() {
        val missing = File("does-not-exist-${System.nanoTime()}")
        assertFalse(isTextMateGrammarCacheFresh(missing, nowMillis = 1_000))

        withCacheFile { cacheFile ->
            cacheFile.setLastModified(2_000)
            assertFalse(isTextMateGrammarCacheFresh(cacheFile, nowMillis = 1_000))
        }
    }

    private fun withCacheFile(block: (File) -> Unit) {
        val file = File.createTempFile("textmate-grammar", ".cache")
        try {
            block(file)
        } finally {
            file.delete()
        }
    }
}
