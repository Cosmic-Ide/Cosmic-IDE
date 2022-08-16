package org.cosmic.ide.preference

import org.cosmic.ide.R
import org.cosmic.ide.ui.utils.DarkTheme

object Settings {

    val MD3: SettingLiveData<Boolean> = BooleanSettingLiveData(R.string.pref_key_md3, R.bool.pref_default_value_md3)
    val DARK_THEME: SettingLiveData<DarkTheme> = EnumSettingLiveData(R.string.pref_key_dark_theme, R.string.pref_default_value_dark_theme, DarkTheme::class.java)
}