package org.cosmic.ide.common.util

import dalvik.system.BaseDexClassLoader

/**
 * Basically a class to load multiple dex files
 *
 * @source https://github.com/Blokkok/blokkok-modsys/blob/main/module-system/src/main/java/com/blokkok/modsys/MultipleDexClassLoader.kt
 */
class MultipleDexClassLoader(private val librarySearchPath: String? = null) {
    val loader by lazy {
        BaseDexClassLoader("", null, librarySearchPath, javaClass.classLoader)
    }

    // we're calling an internal API for adding the dex path, might not be good
    private val addDexPath = BaseDexClassLoader::class.java
        .getMethod("addDexPath", String::class.java)

    fun loadDex(dexPath: String): BaseDexClassLoader {
        addDexPath.invoke(loader, dexPath)

        return loader
    }
}