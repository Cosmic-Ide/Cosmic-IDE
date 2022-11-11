package org.cosmic.ide.util

import android.content.res.Resources
import android.util.TypedValue

object dpToPx {
    var resources: Resources? = null
        set(value) {
            resources = value
        }

    @JvmStatic
    fun dpToPx(dp: Float): Int {
        return Math.round(
            TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, resources?.getDisplayMetrics()
            )
        )
    }
}
