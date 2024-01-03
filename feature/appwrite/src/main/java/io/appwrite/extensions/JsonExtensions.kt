package io.appwrite.extensions

import com.google.gson.Gson

val gson = Gson()

fun Any.toJson(): String =
    gson.toJson(this)

fun <T> String.fromJson(clazz: Class<T>): T =
    gson.fromJson(this, clazz)

inline fun <reified T> String.fromJson(): T =
    gson.fromJson(this, T::class.java)

fun <T> Any.jsonCast(to: Class<T>): T =
    toJson().fromJson(to)

inline fun <reified T> Any.jsonCast(): T =
    toJson().fromJson(T::class.java)

fun <T> Any.tryJsonCast(to: Class<T>): T? = try {
    toJson().fromJson(to)
} catch (ex: Exception) {
    ex.printStackTrace()
    null
}

inline fun <reified T> Any.tryJsonCast(): T? = try {
    toJson().fromJson(T::class.java)
} catch (ex: Exception) {
    ex.printStackTrace()
    null
}
