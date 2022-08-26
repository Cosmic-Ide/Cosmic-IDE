package androidx.appcompat.app

import android.annotation.SuppressLint
import android.content.Context

object AppCompatDelegateCompat {

    @SuppressLint("RestrictedApi")
    fun mapNightMode(delegate: AppCompatDelegate, context: Context, mode: Int): Int {
        return (delegate as AppCompatDelegateImpl).mapNightMode(context, mode)
    }
}