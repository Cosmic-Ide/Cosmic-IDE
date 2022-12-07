package org.cosmic.ide.compiler

import org.cosmic.ide.android.task.dex.D8Task
import org.cosmic.ide.android.task.java.JavaCompiler
import org.cosmic.ide.android.task.kotlin.KotlinCompiler

data class Compilers(
    val kotlin: KotlinCompiler,
    val java: JavaCompiler,
    val dex: D8Task
)