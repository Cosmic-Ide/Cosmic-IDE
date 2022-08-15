package org.cosmic.ide.ui.utils

import android.app.Activity
import android.content.Context

import androidx.annotation.StyleRes
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData

fun Activity.recreateCompat() {
    ActivityCompat.recreate(this)
}

fun Activity.setThemeCompat(@StyleRes resid: Int) {
    setTheme(resid)
}

@Suppress("UNCHECKED_CAST")
val <T> LiveData<T>.valueCompat: T
    get() = value as T

@RestrictedHiddenApi
private val getThemeResIdMethod by lazyReflectedMethod(Context::class.java, "getThemeResId")

val Context.themeResIdCompat: Int
    @StyleRes
    get() = getThemeResIdMethod.invoke(this) as Int