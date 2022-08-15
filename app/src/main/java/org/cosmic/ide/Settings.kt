package org.cosmic.ide

import org.cosmic.ide.ui.utils.NightMode

object Settings {

    val MD3: SettingLiveData<Boolean> = BooleanSettingLiveData(R.string.pref_key_md3, R.string.pref_default_value_md3)
    val NIGHT_MODE: SettingLiveData<NightMode> = EnumSettingLiveData(R.string.pref_key_night_mode, R.string.pref_default_value_night_mode, NightMode::class.java)

}