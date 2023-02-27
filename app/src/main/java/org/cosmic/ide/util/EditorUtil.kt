package org.cosmic.ide.util

import android.content.Context
import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry

object EditorUtil {
    val javaLanguage: Language
        get() = TextMateLanguage.create(
            "source.java", true
        )

    val smaliLanguage: Language
        get() = TextMateLanguage.create(
            "source.smali", false
        )

    fun getColorScheme(ctx: Context): TextMateColorScheme {
        try {
            val registry = ThemeRegistry.getInstance()
            if (ctx.isDarkMode()) {
                registry.setTheme("darcula")
            } else {
                registry.setTheme("QuietLight")
            }
            return TextMateColorScheme.create(registry)
        } catch (e: Exception) {
            throw IllegalStateException(e)
        }
    }
}
