package org.cosmic.ide

import android.content.Context
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener

import androidx.annotation.AnyRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData

import org.cosmic.ide.compat.PreferenceManagerCompat
import org.cosmic.ide.ui.utils.defaultSharedPreferences

abstract class SettingLiveData<T>(
    nameSuffix: String?,
    @StringRes keyRes: Int,
    keySuffix: String?,
    @AnyRes private val defaultValueRes: Int
) : LiveData<T>(), OnSharedPreferenceChangeListener {

    private val sharedPreferences = getSharedPreferences(nameSuffix)
    private val key = getKey(keyRes, keySuffix)
    private var defaultValue: T? = null

    constructor(@StringRes keyRes: Int, @AnyRes defaultValueRes: Int) : this(
        null, keyRes, null, defaultValueRes
    )

    protected fun init() {
        defaultValue = getDefaultValue(defaultValueRes)
        loadValue()
        // Only a weak reference is stored so we don't need to worry about unregistering.
        sharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    private fun getSharedPreferences(nameSuffix: String?): SharedPreferences =
        if (nameSuffix == null) {
            defaultSharedPreferences
        } else {
            val name = "${PreferenceManagerCompat.getDefaultSharedPreferencesName(ApplicationLoader.applicationContext())}_$nameSuffix"
            val mode =  PreferenceManagerCompat.defaultSharedPreferencesMode
            ApplicationLoader.applicationContext().getSharedPreferences(name, mode)
        }

    private fun getKey(@StringRes keyRes: Int, keySuffix: String?): String {
        val key = ApplicationLoader.applicationContext().getString(keyRes)
        return if (keySuffix != null) "${key}_$keySuffix" else key
    }

    protected abstract fun getDefaultValue(@AnyRes defaultValueRes: Int): T

    private fun loadValue() {
        @Suppress("UNCHECKED_CAST")
        value = getValue(sharedPreferences, key, defaultValue as T)
    }

    protected abstract fun getValue(
        sharedPreferences: SharedPreferences,
        key: String,
        defaultValue: T
    ): T

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        if (key == this.key) {
            loadValue()
        }
    }

    fun putValue(value: T) {
        putValue(sharedPreferences, key, value)
    }

    protected abstract fun putValue(sharedPreferences: SharedPreferences, key: String, value: T)
}