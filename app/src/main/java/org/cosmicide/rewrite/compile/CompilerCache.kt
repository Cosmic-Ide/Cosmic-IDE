package org.cosmicide.rewrite.compile

import org.cosmicide.build.dex.D8Task
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.build.kotlin.KotlinCompiler

data class CompilerCache(
    val javaCompiler: JavaCompileTask,
    val kotlinCompiler: KotlinCompiler,
    val dexTask: D8Task
)