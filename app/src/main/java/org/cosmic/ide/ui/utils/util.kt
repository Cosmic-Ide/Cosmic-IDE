package org.cosmic.ide.ui.utils

import android.content.SharedPreferences

import androidx.lifecycle.LiveData
import androidx.preference.PreferenceManager

import org.cosmic.ide.ApplicationLoader

@Suppress("UNCHECKED_CAST")
val <T> LiveData<T>.valueCompat: T
    get() = value as T

val defaultSharedPreferences: SharedPreferences by lazy {
    PreferenceManager.getDefaultSharedPreferences(ApplicationLoader.applicationContext())
}