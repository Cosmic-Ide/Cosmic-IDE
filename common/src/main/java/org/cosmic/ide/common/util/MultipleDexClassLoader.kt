package org.cosmic.ide.common.util

import dalvik.system.DexClassLoader

/**
 * A class to load multiple DEX files
 */
class MultipleDexClassLoader {
    var loader = ClassLoader.getSystemClassLoader()

    fun loadDex(dexPath: String) {
        loader = DexClassLoader(dexPath, null, null, loader)
    }
}
