package org.cosmic.ide.util

import android.app.Activity
import android.content.Context
import androidx.annotation.StyleRes
import androidx.core.app.ActivityCompat

fun Activity.recreateCompat() {
    ActivityCompat.recreate(this)
}

fun Activity.setThemeCompat(@StyleRes resid: Int) {
    setTheme(resid)
}

private val getThemeResIdMethod by lazyReflectedMethod(Context::class.java, "getThemeResId")

val Context.themeResIdCompat: Int
    @StyleRes
    get() = getThemeResIdMethod.invoke(this) as Int