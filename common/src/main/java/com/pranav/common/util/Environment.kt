package com.itsaky.androidide.utils

import java.io.File

/*
 * Required by nb-javac-android
 */
 object Environment {
    @JvmStatic
    lateinit var COMPILER_MODULE: File

    @JvmStatic
    fun init(f: File) {
        COMPILER_MODULE = f
    }
}
