package org.cosmic.ide.compat

import android.content.Context
import androidx.annotation.StyleRes
import org.cosmic.ide.ui.utils.lazyReflectedMethod

private val getThemeResIdMethod by lazyReflectedMethod(Context::class.java, "getThemeResId")

val Context.themeResIdCompat: Int
    @StyleRes
    get() = getThemeResIdMethod.invoke(this) as Int
