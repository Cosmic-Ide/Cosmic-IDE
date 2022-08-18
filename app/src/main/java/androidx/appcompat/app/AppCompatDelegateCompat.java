package androidx.appcompat.app;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;

public class AppCompatDelegateCompat {
    private AppCompatDelegateCompat() {}

    @SuppressLint("RestrictedApi")
    public static int mapNightMode(@NonNull AppCompatDelegate delegate, @NonNull Context context,
                                   int mode) {
        return ((AppCompatDelegateImpl) delegate).mapNightMode(context, mode);
    }
}