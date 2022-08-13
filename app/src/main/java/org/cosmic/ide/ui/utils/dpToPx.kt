package org.cosmic.ide.ui.utils

import android.content.res.Resources
import android.util.TypedValue

object dpToPx {
    private lateinit var resources: Resources

    @JvmStatic
    fun initalizeResources(res: Resources) {
        resources = res
    }

    @JvmStatic
    fun dpToPx(dp: Float): Int {
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, resources.getDisplayMetrics()
            )
        )
    }
}
