package com.pranav.java.ide.ui.utils;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class dpToPx {
    private static Resources res;

    public static void initalizeContext(Context context) {
        res = context.getResources();
    }

    public static int dpToPx(float dp) {

        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP,
                        dp,
                        res.getDisplayMetrics()));
    }
}
