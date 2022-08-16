package org.cosmic.ide.ui.theme

import androidx.appcompat.app.AppCompatDelegate

enum class DarkTheme(val value: Int) {
    FOLLOW_SYSTEM(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM),
    OFF(AppCompatDelegate.MODE_NIGHT_NO),
    ON(AppCompatDelegate.MODE_NIGHT_YES);
}