package org.cosmicide.rewrite.compile

import org.cosmicide.build.dex.D8Task
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.build.kotlin.KotlinCompiler

/**
 * Singleton object to manage the caching of compiler instances.
 */
object CompilerCache {

    val cacheMap = mutableMapOf<Class<*>, Any>()

    fun <T> saveCache(compiler: T) {
        cacheMap[compiler::class.java] = compiler
    }

    inline fun <reified T> getCache(): T {
        return cacheMap[T::class.java] as T
    }
}