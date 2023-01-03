package com.intellij.util.containers;

import android.annotation.SuppressLint;

import java.lang.reflect.Field;

import sun.misc.Unsafe;

public class UnsafeUtil {

    public static Unsafe findUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            Class<Unsafe> type = Unsafe.class;
            try {
                @SuppressLint("DiscouragedPrivateApi") Field field = type.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return type.cast(field.get(type));
            } catch (Exception e) {
                for (Field field : type.getDeclaredFields()) {
                    if (type.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        try {
                            return type.cast(field.get(type));
                        } catch (IllegalAccessException iae) {
                            throw new RuntimeException(iae);
                        }
                    }
                }
            }
            throw new RuntimeException("Unsafe unavailable");
        }
    }
}
