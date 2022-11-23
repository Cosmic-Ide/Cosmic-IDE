package org.cosmic.ide.util

import io.github.rosemoe.sora.lang.Language
import io.github.rosemoe.sora.langs.textmate.TextMateLanguage
import io.github.rosemoe.sora.langs.textmate.TextMateColorScheme
import io.github.rosemoe.sora.langs.textmate.registry.ThemeRegistry
import org.cosmic.ide.App

object EditorUtil {
    val javaLanguage: Language
        get() = TextMateLanguage.create(
            "source.java", true
        )

    val smaliLanguage: Language
        get() = TextMateLanguage.create(
            "source.smali", true
        )

    val colorScheme: TextMateColorScheme
        get() {
            try {
                val registry = ThemeRegistry.getInstance()
                if (App.context.isDarkMode()) {
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