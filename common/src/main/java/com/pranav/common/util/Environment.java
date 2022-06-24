package com.itsaky.androidide.utils;

import java.io.File;

/*
 * Required by nb-javac-android
 */
public class Environment {
    public static File COMPILER_MODULE;

    public static void init(File f) {
        COMPILER_MODULE = f;
    }
}
