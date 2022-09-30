package org.cosmic.ide.ui.preference

import android.content.SharedPreferences
import androidx.annotation.BoolRes
import androidx.annotation.StringRes
import androidx.core.content.edit
import org.cosmic.ide.ApplicationLoader

class BooleanSettingLiveData(
    @StringRes keyRes: Int,
    keySuffix: String?,
    @BoolRes defaultValueRes: Int
) : SettingLiveData<Boolean>(keyRes, keySuffix, defaultValueRes) {
    constructor(@StringRes keyRes: Int, @BoolRes defaultValueRes: Int) : this(
        keyRes, null, defaultValueRes
    )

    init {
        init()
    }

    override fun getDefaultValue(@BoolRes defaultValueRes: Int): Boolean =
        ApplicationLoader.applicationContext().resources.getBoolean(defaultValueRes)

    override fun getValue(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: Boolean
    ): Boolean = sharedPreferences.getBoolean(key, defaultValue)

    override fun putValue(sharedPreferences: SharedPreferences, key: String, value: Boolean) {
        sharedPreferences.edit { putBoolean(key, value) }
    }
}

// Use string resource for default value so that we can support ListPreference.
class EnumSettingLiveData<E : Enum<E>>(
    @StringRes keyRes: Int,
    keySuffix: String?,
    @StringRes defaultValueRes: Int,
    enumClass: Class<E>
) : SettingLiveData<E>(keyRes, keySuffix, defaultValueRes) {
    private val enumValues = enumClass.enumConstants!!

    constructor(
        @StringRes keyRes: Int,
        @StringRes defaultValueRes: Int,
        enumClass: Class<E>
    ) : this(keyRes, null, defaultValueRes, enumClass)

    init {
        init()
    }

    override fun getDefaultValue(@StringRes defaultValueRes: Int): E =
        enumValues[ApplicationLoader.applicationContext().getString(defaultValueRes).toInt()]

    override fun getValue(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: E
    ): E {
        val valueOrdinal = sharedPreferences.getString(key, null)?.toInt() ?: return defaultValue
        return if (valueOrdinal in enumValues.indices) enumValues[valueOrdinal] else defaultValue
    }

    override fun putValue(sharedPreferences: SharedPreferences, key: String, value: E) {
        sharedPreferences.edit { putString(key, value.ordinal.toString()) }
    }
}
