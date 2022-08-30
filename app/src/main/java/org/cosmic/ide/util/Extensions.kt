package org.cosmic.ide.util

import android.app.Activity
import android.content.Context
import androidx.annotation.StyleRes
import androidx.core.app.ActivityCompat
import androidx.lifecycle.LiveData

private val getThemeResIdMethod by lazyReflectedMethod(Context::class.java, "getThemeResId")

fun Activity.recreateCompat() {
    ActivityCompat.recreate(this)
}

fun Activity.setThemeCompat(@StyleRes resid: Int) {
    setTheme(resid)
}

val Context.themeResIdCompat: Int
    @StyleRes
    get() = getThemeResIdMethod.invoke(this) as Int

@Suppress("UNCHECKED_CAST")
val <T> LiveData<T>.valueCompat: T
    get() = value as T

val Boolean.int
    get() = if (this) 1 else 0