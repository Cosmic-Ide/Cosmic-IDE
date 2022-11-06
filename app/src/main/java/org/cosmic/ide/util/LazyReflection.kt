package org.cosmic.ide.util

import java.lang.reflect.Field
import java.lang.reflect.Method
import kotlin.reflect.KClass

/** Lazily reflect to retrieve a [Field]. */
fun lazyReflectedField(clazz: KClass<*>, field: String) = lazy {
    clazz.java.getDeclaredField(field).also { it.isAccessible = true }
}

/** Lazily reflect to retrieve a [Method]. */
fun lazyReflectedMethod(clazz: KClass<*>, method: String) = lazy {
    clazz.java.getDeclaredMethod(method).also { it.isAccessible = true }
}
