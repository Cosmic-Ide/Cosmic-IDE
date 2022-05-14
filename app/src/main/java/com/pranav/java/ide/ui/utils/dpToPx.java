package com.pranav.java.ide.ui.utils;

import android.content.res.Resources;
import android.util.TypedValue;

public class dpToPx {
    private static Resources resources;

    public static void initalizeResources(Resources res) {
        resources = res;
    }

    public static int dpToPx(float dp) {

        return Math.round(
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, dp, res.getDisplayMetrics()));
    }
}
