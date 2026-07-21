package org.cosmicide.editor.lsp

import java.io.File

internal const val TEXT_MATE_GRAMMAR_CACHE_MAX_AGE_MILLIS = 7L * 24 * 60 * 60 * 1000

internal fun isTextMateGrammarCacheFresh(
    cacheFile: File,
    nowMillis: Long = System.currentTimeMillis()
): Boolean {
    if (!cacheFile.isFile) return false

    val ageMillis = nowMillis - cacheFile.lastModified()
    return ageMillis in 0..TEXT_MATE_GRAMMAR_CACHE_MAX_AGE_MILLIS
}
