package org.cosmic.ide.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources.Theme
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import androidx.core.view.*

fun Context.isDarkMode(): Boolean {
    val darkModeFlag = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
    return darkModeFlag == Configuration.UI_MODE_NIGHT_YES
}

@JvmOverloads
fun Context.resolveAttr(id: Int, resolveRefs: Boolean = true): Int {
    return theme.resolveAttr(id, resolveRefs)
}

@JvmOverloads
fun Theme.resolveAttr(id: Int, resolveRefs: Boolean = true): Int =
    TypedValue().let {
        resolveAttribute(id, it, resolveRefs)
        it.data
    }

@JvmOverloads
fun View.addSystemWindowInsetToPadding(
    left: Boolean = false,
    top: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false
) {
    val (initialLeft, initialTop, initialRight, initialBottom) =
        listOf(paddingLeft, paddingTop, paddingRight, paddingBottom)

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(
            left = initialLeft + if (left) insets.left else 0,
            top = initialTop + if (top) insets.top else 0,
            right = initialRight + if (right) insets.right else 0,
            bottom = initialBottom + if (bottom) insets.bottom else 0
        )
        windowInsets
    }
}

@JvmOverloads
fun View.addSystemWindowInsetToMargin(
    left: Boolean = false,
    top: Boolean = false,
    right: Boolean = false,
    bottom: Boolean = false
) {
    val (initialLeft, initialTop, initialRight, initialBottom) =
        listOf(marginLeft, marginTop, marginRight, marginBottom)

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            updateMargins(
                left = initialLeft + if (left) insets.left else 0,
                top = initialTop + if (top) insets.top else 0,
                right = initialRight + if (right) insets.right else 0,
                bottom = initialBottom + if (bottom) insets.bottom else 0
            )
        }
        windowInsets
    }
}
