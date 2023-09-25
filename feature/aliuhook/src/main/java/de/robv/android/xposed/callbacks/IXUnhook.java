/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 *
 * Originally written by rovo89 as part of the original Xposed
 * Copyright 2013 rovo89, Tungstwenty
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0
 */

package de.robv.android.xposed.callbacks;

/**
 * Interface for objects that can be used to remove callbacks.
 *
 * @param <T> The class of the callback.
 */
@SuppressWarnings({"unused"})
public interface IXUnhook<T> {
    /**
     * Returns the callback that has been registered.
     */
    T getCallback();

    /**
     * Removes the callback.
     */
    void unhook();
}