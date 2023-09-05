/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.util

import dalvik.system.BaseDexClassLoader
import java.io.File

/**
 * Basically a class to load multiple dex files
 *
 * @source https://github.com/Blokkok/blokkok-modsys/blob/main/module-system/src/main/java/com/blokkok/modsys/MultipleDexClassLoader.kt
 */
class MultipleDexClassLoader(
    private val librarySearchPath: String? = null,
    classLoader: ClassLoader = ClassLoader.getSystemClassLoader()
) {
    val loader by lazy {
        BaseDexClassLoader("", null, librarySearchPath, classLoader)
    }

    // we're calling an internal API for adding the dex path, might not be good
    private val addDexPath = BaseDexClassLoader::class.java
        .getMethod("addDexPath", String::class.java)

    fun loadDex(dexPath: String): BaseDexClassLoader {
        addDexPath.invoke(loader, dexPath)

        return loader
    }

    fun loadDex(dexFile: File) {
        loadDex(dexFile.absolutePath)
    }

    companion object {
        @JvmStatic
        val INSTANCE = MultipleDexClassLoader(classLoader = Companion::class.java.classLoader!!)
    }
}