package com.pranav.java.ide.ui.utils

import android.content.Context
import android.view.View
import android.view.ViewGroup
import com.pranav.java.ide.ui.utils.dpToPx;

fun toDp(dp: Int) = dpToPx.dpToPx(dp.toFloat())

fun View.setMargins(left: Int? = null, top: Int? = null, right: Int? = null, bottom: Int? = null) {
    val params = layoutParams as? ViewGroup.MarginLayoutParams
    params?.setMargins(
        left ?: params.leftMargin,
        top ?: params.topMargin,
        right ?: params.rightMargin,
        bottom ?: params.bottomMargin)
    layoutParams = params
}