package org.cosmic.ide

import android.content.SharedPreferences
import android.os.Parcel

import androidx.annotation.AnyRes
import androidx.annotation.ArrayRes
import androidx.annotation.BoolRes
import androidx.annotation.DimenRes
import androidx.annotation.IntegerRes
import androidx.annotation.StringRes

import org.cosmic.ide.ApplicationLoader.context

class BooleanSettingLiveData(
    nameSuffix: String?,
    @StringRes keyRes: Int,
    keySuffix: String?,
    @BoolRes defaultValueRes: Int
) : SettingLiveData<Boolean>(nameSuffix, keyRes, keySuffix, defaultValueRes) {
    constructor(@StringRes keyRes: Int, @BoolRes defaultValueRes: Int) : this(
        null, keyRes, null, defaultValueRes
    )

    init {
        init()
    }

    override fun getDefaultValue(@BoolRes defaultValueRes: Int): Boolean =
        context.getBoolean(defaultValueRes)

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
    nameSuffix: String?,
    @StringRes keyRes: Int,
    keySuffix: String?,
    @StringRes defaultValueRes: Int,
    enumClass: Class<E>
) : SettingLiveData<E>(nameSuffix, keyRes, keySuffix, defaultValueRes) {
    private val enumValues = enumClass.enumConstants!!

    constructor(
        @StringRes keyRes: Int,
        @StringRes defaultValueRes: Int,
        enumClass: Class<E>
    ) : this(null, keyRes, null, defaultValueRes, enumClass)

    init {
        init()
    }

    override fun getDefaultValue(@StringRes defaultValueRes: Int): E =
        enumValues[context.getString(defaultValueRes).toInt()]

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