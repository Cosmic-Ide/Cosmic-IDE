package com.pranav.java.ide.ui.utils;

import android.content.Context;
import android.util.TypedValue;

public class dpToPx {
    private static Context mContext;

    public static void initalizeContext(Context context) {
        mContext = context;
    }

    public static int dpToPx(float dp) {

        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, mContext.getResources().getDisplayMetrics()));
    }
}
