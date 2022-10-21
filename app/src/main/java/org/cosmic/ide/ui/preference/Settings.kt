package org.cosmic.ide.ui.preference

import org.cosmic.ide.R
import org.cosmic.ide.ui.theme.DarkTheme

object Settings {
    val DARK_THEME: SettingLiveData<DarkTheme> = EnumSettingLiveData(R.string.pref_key_dark_theme, R.string.pref_default_value_dark_theme, DarkTheme::class.java)
}
