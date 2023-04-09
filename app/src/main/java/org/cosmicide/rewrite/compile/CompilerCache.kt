package org.cosmicide.rewrite.compile

import org.cosmicide.build.dex.D8Task
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.build.kotlin.KotlinCompiler

/**
 * A data class that holds instances of [JavaCompileTask], [KotlinCompiler], and [D8Task].
 * Used to cache these instances for efficient re-use.
 *
 * @property javaCompiler an instance of [JavaCompileTask]
 * @property kotlinCompiler an instance of [KotlinCompiler]
 * @property dexTask an instance of [D8Task]
 */
object CompilerCache {

    val cacheMap = mutableMapOf<Class<*>, Any>()
    fun <T> saveCache(compiler: T) {
        cacheMap[compiler!!::class.java] = compiler
    }

    inline fun <reified T> getCache(): T {
        return cacheMap[T::class.java] as T
    }
}