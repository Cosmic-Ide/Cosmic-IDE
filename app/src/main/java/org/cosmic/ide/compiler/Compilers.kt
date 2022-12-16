package org.cosmic.ide.compiler

import org.cosmic.ide.android.task.dex.D8Task
import org.cosmic.ide.android.task.java.JavaCompiler

data class Compilers(
    val java: JavaCompiler,
    val dex: D8Task
)
