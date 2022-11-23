package com.intellij.openapi.editor.impl;

import com.intellij.util.ArrayFactory;
import com.intellij.util.ArrayUtil;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Maintains an atomic immutable array of listeners of type {@code T} in sorted order according to
 * {@link #comparator} N.B. internal array is exposed for faster iterating listeners in to- and
 * reverse order, so care should be taken for not mutating it by clients
 */
class LockFreeCOWSortedArray<T> extends AtomicReference<T[]> {
    @NotNull private final Comparator<? super T> comparator;
    private final @NotNull ArrayFactory<? extends T> arrayFactory;

    LockFreeCOWSortedArray(
            @NotNull Comparator<? super T> comparator,
            @NotNull ArrayFactory<? extends T> arrayFactory) {
        this.comparator = comparator;
        this.arrayFactory = arrayFactory;
        set(arrayFactory.create(0));
    }

    // returns true if changed
    void add(@NotNull T element) {
        while (true) {
            T[] oldArray = get();
            int i = insertionIndex(oldArray, element);
            T[] newArray = ArrayUtil.insert(oldArray, i, element);
            if (compareAndSet(oldArray, newArray)) break;
        }
    }

    boolean remove(@NotNull T listener) {
        while (true) {
            T[] oldArray = get();
            T[] newArray = ArrayUtil.remove(oldArray, listener, arrayFactory);
            //noinspection ArrayEquality
            if (oldArray == newArray) return false;
            if (compareAndSet(oldArray, newArray)) break;
        }
        return true;
    }

    private int insertionIndex(T[] elements, @NotNull T e) {
        for (int i = 0; i < elements.length; i++) {
            T element = elements[i];
            if (comparator.compare(e, element) < 0) {
                return i;
            }
        }
        return elements.length;
    }

    T[] getArray() {
        return get();
    }
}
