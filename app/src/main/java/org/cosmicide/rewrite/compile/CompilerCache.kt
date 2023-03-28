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
data class CompilerCache(
    val javaCompiler: JavaCompileTask,
    val kotlinCompiler: KotlinCompiler,
    val dexTask: D8Task
)