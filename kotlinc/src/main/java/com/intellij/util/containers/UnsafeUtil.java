package com.intellij.util.containers;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public class UnsafeUtil {

    public static Unsafe findUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            Class<Unsafe> type = Unsafe.class;
            try {
                Field field = type.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                return type.cast(field.get(type));
            } catch (Exception e) {
                for (Field field : type.getDeclaredFields()) {
                    if (type.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        try {
                            return type.cast(field.get(type));
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
            throw new RuntimeException("Unsafe unavailable");
        }
    }
}
