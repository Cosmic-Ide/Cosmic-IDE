package org.cosmic.ide.common.util

import dalvik.system.BaseDexClassLoader
import java.io.File

class MultipleDexClassLoader(private val dexPaths: List<String>) :
    BaseDexClassLoader(joinDexPath(dexPaths), null, null, javaClass.classLoader) {
    companion object {
        private fun joinDexPath(dexPaths: List<String>): String {
            return dexPaths.joinToString(File.pathSeparator)
        }
    }
}
