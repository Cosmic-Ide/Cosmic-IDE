package org.cosmicide.rewrite.extension

import android.content.Context
import android.util.TypedValue

fun Context.getDip(input: Float): Float {
    return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, input, resources.displayMetrics)
}