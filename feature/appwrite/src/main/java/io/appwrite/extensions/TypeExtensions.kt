package io.appwrite.extensions

import kotlin.reflect.KClass
import kotlin.reflect.typeOf

inline fun <reified T : Any> classOf(): Class<T> {
    return (typeOf<T>().classifier!! as KClass<T>).java
}