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
 * Base class for Xposed callbacks.
 * <p>
 * The actual (abstract) callback methods are added by subclasses.
 */
@SuppressWarnings({"unused", "JavaDoc"})
public abstract class XCallback implements Comparable<XCallback> {
    /**
     * Callback priority, higher number means earlier execution.
     *
     * <p>This is usually set to {@link #PRIORITY_DEFAULT}. However, in case a certain callback should
     * be executed earlier or later a value between {@link #PRIORITY_HIGHEST} and {@link #PRIORITY_LOWEST}
     * can be set instead. The values are just for orientation though, Xposed doesn't enforce any
     * boundaries on the priority values.
     */
    public final int priority;

    /**
     * @deprecated This constructor can't be hidden for technical reasons. Nevertheless, don't use it!
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public XCallback() {
        this.priority = PRIORITY_DEFAULT;
    }

    /**
     * @hide
     */
    public XCallback(int priority) {
        this.priority = priority;
    }

    /**
     * Base class for Xposed callback parameters.
     */
    public static abstract class Param {
        protected Param() {
        }
    }

    /**
     * @hide
     */
    @Override
    public int compareTo(XCallback other) {
        if (this == other)
            return 0;

        // order descending by priority
        if (other.priority != this.priority)
            return other.priority - this.priority;
            // then randomly
        else if (System.identityHashCode(this) < System.identityHashCode(other))
            return -1;
        else
            return 1;
    }

    /**
     * The default priority, see {@link #priority}.
     */
    public static final int PRIORITY_DEFAULT = 50;

    /**
     * Execute this callback late, see {@link #priority}.
     */
    public static final int PRIORITY_LOWEST = -10000;

    /**
     * Execute this callback early, see {@link #priority}.
     */
    public static final int PRIORITY_HIGHEST = 10000;
}