package org.jetbrains.kotlin.com.intellij.util.containers;

import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;

public class UnsafeUtil {

    public static Unsafe findUnsafe() {
        try {
            return Unsafe.getUnsafe();
        } catch (SecurityException se) {
            return AccessController.doPrivileged(
                    (PrivilegedAction<Unsafe>)
                            () -> {
                                try {
                                    Class<Unsafe> type = Unsafe.class;
                                    try {
                                        Field field = type.getDeclaredField("theUnsafe");
                                        field.setAccessible(true);
                                        return type.cast(field.get(type));
                                    } catch (Exception e) {
                                        for (Field field : type.getDeclaredFields()) {
                                            if (type.isAssignableFrom(field.getType())) {
                                                field.setAccessible(true);
                                                return type.cast(field.get(type));
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException("Unsafe unavailable", e);
                                }
                                throw new RuntimeException("Unsafe unavailable");
                            });
        }
    }
}
