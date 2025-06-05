/*
 * This file is part of AliuHook, a library providing XposedAPI bindings to LSPlant
 * Copyright (c) 2021 Juby210 & Vendicated
 * Licensed under the Open Software License version 3.0
 *
 * Originally written by rovo89 as part of the original Xposed
 * Copyright 2013 rovo89, Tungstwenty
 * Licensed under the Apache License, Version 2.0, see http://www.apache.org/licenses/LICENSE-2.0
 */

package de.robv.android.xposed;

import android.util.Log;

import java.lang.reflect.*;
import java.util.*;

@SuppressWarnings({"unused", "JavaDoc"})
public class XposedBridge {
    private static final String TAG = "AliuHook-XposedBridge";

    static {
        try {
            callbackMethod = XposedBridge.HookInfo.class.getMethod("callback", Object[].class);
        } catch (Throwable t) {
            throw new RuntimeException("Failed to initialize", t);
        }

        System.loadLibrary("aliuhook");
    }

    private static final Object[] EMPTY_ARRAY = new Object[0];

    private static final Map<Member, HookInfo> hookRecords = new HashMap<>();
    private static final Method callbackMethod;

    private static native Method hook0(Object context, Member original, Method callback);

    private static native boolean unhook0(Member target);

    private static native boolean deoptimize0(Member target);

    private static native boolean makeClassInheritable0(Class<?> target);

    private static native Object allocateInstance0(Class<?> clazz);

    private static native boolean invokeConstructor0(Object instance, Constructor<?> constructor, Object[] args);

    // Not used for now
    private static native boolean isHooked0(Member target);

    /**
     * Disable profile saver to try to prevent ART ahead of time compilation
     * which may lead to aggressive method inlining, thus resulting in those
     * methods being unhookable unless you first call {@link #deoptimizeMethod(Member)} on all callers
     * of that method.
     * <p>
     * You could also try deleting /data/misc/profiles/cur/0/com.YOURPACKAGE/primary.prof
     * <p>
     * See https://source.android.com/devices/tech/dalvik/configure#how_art_works for more info
     *
     * @return Whether disabling profile saver succeeded
     */
    public static native boolean disableProfileSaver();

    /**
     * Disables HiddenApi restrictions, thus allowing you access to all private interfaces.
     * <p>
     *
     * @return Whether disabling hidden api succeeded
     * @see <a href="https://developer.android.com/guide/app-compatibility/restrictions-non-sdk-interfaces">https://developer.android.com/guide/app-compatibility/restrictions-non-sdk-interfaces</a>
     */
    public static native boolean disableHiddenApiRestrictions();

    private static void checkMethod(Member method) {
        if (method == null)
            throw new NullPointerException("method must not be null");
        if (!(method instanceof Method || method instanceof Constructor<?>))
            throw new IllegalArgumentException("method must be a Method or Constructor");

        var modifiers = method.getModifiers();
        if (Modifier.isAbstract(modifiers))
            throw new IllegalArgumentException("method must not be abstract");
    }

    /**
     * Check if a method is hooked
     * @param method The method to check
     * @return true if method is hooked
     */
    public static boolean isHooked(Member method) {
        return hookRecords.containsKey(method);
    }

    /**
     * Make a final class inheritable. Removes final modifier from class and its constructors and makes
     * constructors accessible (private -> protected)
     *
     * @param clazz Class to make inheritable
     */
    public static boolean makeClassInheritable(Class<?> clazz) {
        if (clazz == null) throw new NullPointerException("class must not be null");

        return makeClassInheritable0(clazz);
    }

    /**
     * Deoptimize a method to avoid inlining
     *
     * @param method The method to deoptimize. Generally it should be a caller of a method
     *               that is inlined.
     */
    public static boolean deoptimizeMethod(Member method) {
        checkMethod(method);
        return deoptimize0(method);
    }

    /**
     * Hook any method (or constructor) with the specified callback.
     *
     * @param method   The method to be hooked.
     * @param callback The callback to be executed when the hooked method is called.
     * @return An object that can be used to remove the hook.
     */
    public static XC_MethodHook.Unhook hookMethod(Member method, XC_MethodHook callback) {
        checkMethod(method);
        if (callback == null) throw new NullPointerException("callback must not be null");

        HookInfo hookRecord;
        synchronized (hookRecords) {
            hookRecord = hookRecords.get(method);
            if (hookRecord == null) {
                hookRecord = new HookInfo(method);
                var backup = hook0(hookRecord, method, callbackMethod);
                if (backup == null) throw new IllegalStateException("Failed to hook method");
                hookRecord.backup = backup;
                hookRecords.put(method, hookRecord);
            }
        }

        hookRecord.callbacks.add(callback);

        return callback.new Unhook(method);
    }

    /**
     * Hooks all methods with a certain name that were declared in the specified class. Inherited
     * methods and constructors are not considered. For constructors, use
     * {@link #hookAllConstructors} instead.
     *
     * @param hookClass  The class to check for declared methods.
     * @param methodName The name of the method(s) to hook.
     * @param callback   The callback to be executed when the hooked methods are called.
     * @return A set containing one object for each found method which can be used to unhook it.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Set<XC_MethodHook.Unhook> hookAllMethods(Class<?> hookClass, String methodName, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member method : hookClass.getDeclaredMethods())
            if (method.getName().equals(methodName))
                unhooks.add(hookMethod(method, callback));
        return unhooks;
    }

    /**
     * Hook all constructors of the specified class.
     *
     * @param hookClass The class to check for constructors.
     * @param callback  The callback to be executed when the hooked constructors are called.
     * @return A set containing one object for each found constructor which can be used to unhook it.
     */
    @SuppressWarnings("UnusedReturnValue")
    public static Set<XC_MethodHook.Unhook> hookAllConstructors(Class<?> hookClass, XC_MethodHook callback) {
        Set<XC_MethodHook.Unhook> unhooks = new HashSet<>();
        for (Member constructor : hookClass.getDeclaredConstructors())
            unhooks.add(hookMethod(constructor, callback));
        return unhooks;
    }

    /**
     * Removes the callback for a hooked method/constructor.
     *
     * @param method   The method for which the callback should be removed.
     * @param callback The reference to the callback as specified in {@link #hookMethod}.
     * @deprecated Use {@link XC_MethodHook.Unhook#unhook} instead. An instance of the {@code Unhook}
     * class is returned when you hook the method.
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    public static void unhookMethod(Member method, XC_MethodHook callback) {
        synchronized (hookRecords) {
            var record = hookRecords.get(method);
            if (record != null) {
                record.callbacks.remove(callback);
                if (record.callbacks.size() == 0) {
                    hookRecords.remove(method);
                    unhook0(method);
                }
            }
        }
    }

    /**
     * Basically the same as {@link Method#invoke}, but calls the original method
     * as it was before the interception by Xposed. Also, access permissions are not checked.
     *
     * <p class="caution">There are very few cases where this method is needed. A common mistake is
     * to replace a method and then invoke the original one based on dynamic conditions. This
     * creates overhead and skips further hooks by other modules. Instead, just hook (don't replace)
     * the method and call {@code param.setResult(null)} in {@link XC_MethodHook#beforeHookedMethod}
     * if the original method should be skipped.
     *
     * @param method     The method to be called.
     * @param thisObject For non-static calls, the "this" pointer, otherwise {@code null}.
     * @param args       Arguments for the method call as Object[] array.
     * @return The result returned from the invoked method.
     * @throws NullPointerException      if {@code receiver == null} for a non-static method
     * @throws IllegalAccessException    if this method is not accessible (see {@link AccessibleObject})
     * @throws IllegalArgumentException  if the number of arguments doesn't match the number of parameters, the receiver
     *                                   is incompatible with the declaring class, or an argument could not be unboxed
     *                                   or converted by a widening conversion to the corresponding parameter type
     * @throws InvocationTargetException if an exception was thrown by the invoked method
     */
    public static Object invokeOriginalMethod(Member method, Object thisObject, Object[] args)
            throws NullPointerException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        if (args == null)
            args = EMPTY_ARRAY;

        var hookRecord = hookRecords.get(method);
        try {
            // Checking method is not needed if we found hookRecord
            if (hookRecord != null)
                return invokeMethod(hookRecord.backup, thisObject, args);

            checkMethod(method);
            return invokeMethod(method, thisObject, args);
        } catch (InstantiationException ex) {
            // This should never be reached
            throw new IllegalArgumentException("The class this Constructor belongs to is abstract and cannot be instantiated");
        }
    }

    private static Object invokeMethod(Member member, Object thisObject, Object[] args) throws IllegalAccessException, InvocationTargetException, InstantiationException {
        if (member instanceof Method) {
            var method = (Method) member;
            method.setAccessible(true);
            return method.invoke(thisObject, args);
        } else {
            var ctor = (Constructor<?>) member;
            ctor.setAccessible(true);
            return ctor.newInstance(args);
        }
    }

    /**
     * Allocate a class instance without calling any constructors.
     *
     * @param clazz Target class to allocate
     * @noinspection unchecked
     */
    public static <T> T allocateInstance(Class<T> clazz) {
        Objects.requireNonNull(clazz);
        return (T) allocateInstance0(clazz);
    }

    /**
     * Invoke a constructor for an already existing instance.
     * This is most useful in conjunction with {@code sun.misc.Unsafe#allocateInstance(Class)} or
     * {@link XposedBridge#allocateInstance(Class)} in order to control when the constructor gets called.
     *
     * @param instance    A class instance.
     * @param constructor Constructor located on the instance's class or one of its supertypes.
     * @param args        Args matching the constructor, if any. Can be null.
     * @return True if operation was successful
     */
    public static <S, T extends S> boolean invokeConstructor(T instance, Constructor<S> constructor, Object... args) {
        Objects.requireNonNull(instance);
        Objects.requireNonNull(constructor);
        if (constructor.isVarArgs())
            throw new IllegalArgumentException("varargs parameters are not supported");
        if (args.length == 0) args = null;
        return invokeConstructor0(instance, constructor, args);
    }

    /**
     * @hide
     */
    public static final class CopyOnWriteSortedSet<E> {
        private transient volatile Object[] elements = EMPTY_ARRAY;

        // Aliucord added
        public int size() {
            return elements.length;
        }

        @SuppressWarnings("UnusedReturnValue")
        public synchronized boolean add(E e) {
            int index = indexOf(e);
            if (index >= 0)
                return false;

            Object[] newElements = new Object[elements.length + 1];
            System.arraycopy(elements, 0, newElements, 0, elements.length);
            newElements[elements.length] = e;
            Arrays.sort(newElements);
            elements = newElements;
            return true;
        }

        @SuppressWarnings("UnusedReturnValue")
        public synchronized boolean remove(E e) {
            int index = indexOf(e);
            if (index == -1)
                return false;

            Object[] newElements = new Object[elements.length - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index, elements.length - index - 1);
            elements = newElements;
            return true;
        }

        private int indexOf(Object o) {
            for (int i = 0; i < elements.length; i++) {
                if (o.equals(elements[i]))
                    return i;
            }
            return -1;
        }

        public Object[] getSnapshot() {
            return elements;
        }
    }

    // Aliucord changed: public, so that it can be passed as lsplant context object
    public static class HookInfo {
        Member backup;
        private final Member method;
        final CopyOnWriteSortedSet<XC_MethodHook> callbacks = new CopyOnWriteSortedSet<>();
        private final boolean isStatic;
        private final Class<?> returnType;

        public HookInfo(Member method) {
            this.method = method;
            isStatic = Modifier.isStatic(method.getModifiers());
            if (method instanceof Method) {
                var rt = ((Method) method).getReturnType();
                if (!rt.isPrimitive()) {
                    returnType = rt;
                    return;
                }
            }
            returnType = null;
        }

        public Object callback(Object[] args) throws Throwable {
            var param = new XC_MethodHook.MethodHookParam();
            param.method = method;

            if (isStatic) {
                param.thisObject = null;
                param.args = args;
            } else {
                param.thisObject = args[0];
                param.args = new Object[args.length - 1];
                System.arraycopy(args, 1, param.args, 0, args.length - 1);
            }

            var hooks = callbacks.getSnapshot();
            var hookCount = hooks.length;

            // shouldn't happen since 0 remaining callbacks leads to unhook
            if (hookCount == 0) {
                try {
                    return invokeMethod(backup, param.thisObject, param.args);
                } catch (InvocationTargetException e) {
                    //noinspection ConstantConditions
                    throw e.getCause();
                }
            }

            int beforeIdx = 0;
            do {
                try {
                    ((XC_MethodHook) hooks[beforeIdx]).beforeHookedMethod(param);
                } catch (Throwable t) {
                    XposedBridge.log(t);

                    param.setResult(null);
                    param.returnEarly = false;
                    continue;
                }

                if (param.returnEarly) {
                    beforeIdx++;
                    break;
                }
            } while (++beforeIdx < hookCount);

            if (!param.returnEarly) {
                try {
                    param.setResult(invokeMethod(backup, param.thisObject, param.args));
                } catch (InvocationTargetException e) {
                    param.setThrowable(e.getCause());
                }
            }

            int afterIdx = beforeIdx - 1;
            do {
                Object lastResult = param.getResult();
                Throwable lastThrowable = param.getThrowable();

                try {
                    ((XC_MethodHook) hooks[afterIdx]).afterHookedMethod(param);
                } catch (Throwable t) {
                    XposedBridge.log(t);

                    if (lastThrowable == null)
                        param.setResult(lastResult);
                    else
                        param.setThrowable(lastThrowable);
                }
            } while (--afterIdx >= 0);

            var result = param.getResultOrThrowable();
            if (returnType != null)
                result = returnType.cast(result);
            return result;
        }
    }

    // Aliucord changed: private, so people don't abuse this
    private static void log(Throwable t) {
        Log.e(TAG, "Uncaught Exception", t);
    }
}
