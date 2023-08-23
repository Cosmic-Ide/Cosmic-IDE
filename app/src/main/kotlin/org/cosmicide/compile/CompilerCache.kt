/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.compile

import org.cosmicide.build.Task

/**
 * Singleton object to manage the caching of compiler instances.
 */
object CompilerCache {
    @JvmStatic
    val cacheMap = mutableMapOf<Class<*>, Task>()

    @JvmStatic
    fun <T : Task> saveCache(compiler: T) {
        cacheMap[compiler::class.java] = compiler
    }

    @JvmStatic
    inline fun <reified T : Task> getCache(): T {
        return cacheMap[T::class.java] as T
    }
}