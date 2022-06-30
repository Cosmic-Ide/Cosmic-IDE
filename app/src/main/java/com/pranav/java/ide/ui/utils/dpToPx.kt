package com.pranav.java.ide.ui.utils

import android.content.res.Resources
import android.util.TypedValue

object dpToPx {
    @JvmStatic
    private var resources: Resources

    @JvmStatic
    fun initalizeResources(res: Resources) {
        resources = res
    }

    @JvmStatic
    fun dpToPx(dp: float): Int {
        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()))
    }
}
