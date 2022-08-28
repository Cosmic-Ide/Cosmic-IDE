package org.cosmic.ide.util

import androidx.lifecycle.LiveData

@Suppress("UNCHECKED_CAST")
val <T> LiveData<T>.valueCompat: T
    get() = value as T

val Boolean.int
    get() = if (this) 1 else 0
