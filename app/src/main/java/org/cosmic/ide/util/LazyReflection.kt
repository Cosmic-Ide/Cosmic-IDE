package org.cosmic.ide.util

import java.lang.reflect.Method

@Throws(ReflectiveOperationException::class)
private fun getReflectedClass(className: String): Class<*> = Class.forName(className)

@Throws(ReflectiveOperationException::class)
fun lazyReflectedMethod(
    declaringClass: Class<*>,
    methodName: String,
    vararg parameterTypes: Any
): Lazy<Method> = lazy {
    getReflectedMethod(declaringClass, methodName, *getParameterTypes(parameterTypes))
}

@Throws(ReflectiveOperationException::class)
private fun getReflectedMethod(
    declaringClass: Class<*>,
    methodName: String,
    vararg parameterTypes: Class<*>
) = declaringClass.getDeclaredMethod(methodName, *parameterTypes).also { it.isAccessible = true }

@Throws(ReflectiveOperationException::class)
private fun getParameterTypes(parameterTypes: Array<out Any>): Array<Class<*>> =
    Array(parameterTypes.size) {
        when (val parameterType = parameterTypes[it]) {
            is Class<*> -> parameterType
            is String -> getReflectedClass(parameterType)
            else -> throw IllegalArgumentException(parameterType.toString())
        }
    }
