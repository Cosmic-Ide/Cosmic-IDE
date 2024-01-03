/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.completion.java.parser.cache

import javassist.ClassPool
import javassist.CtClass
import javassist.Modifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.jar.JarFile

class SymbolCacher(private val jarFile: File) {
    private val cache = ConcurrentHashMap<String, CtClass>()
    private val packageCache = ConcurrentHashMap<String, String>()
    private val scope = CoroutineScope(Dispatchers.IO)

    fun getClass(className: String): CtClass? {
        return cache[className]
    }

    fun loadClassesFromJar(): Map<String, CtClass> {
        val classPool = ClassPool.getDefault()
        val file = JarFile(jarFile)
        file.use {
            val deferredList = it.entries().asSequence()
                .map { entry ->
                    scope.async {
                        if (entry.name.endsWith(".class").not()) return@async
                        val packageName = entry.name.substringBeforeLast('/').replace('/', '.')
                        if (packageCache.containsKey(packageName).not()) {
                            packageCache[packageName] = packageName
                        }
                        val ctClass = classPool.makeClass(file.getInputStream(entry))
                        if (isPublicStaticClass(ctClass)) {
                            cache[ctClass.name] = ctClass
                        }
                    }
                }
                .toList()

            runBlocking {
                deferredList.awaitAll()
            }
        }

        return cache
    }

    private fun isPublicStaticClass(ctClass: CtClass): Boolean {
        val modifiers = ctClass.modifiers
        return Modifier.isPublic(modifiers) || Modifier.isStatic(modifiers)
    }

    fun removeClass(className: String) {
        cache.remove(className)
    }

    fun clearCache() {
        cache.clear()
    }

    fun filterClassNames(prefix: String): Map<String, String> {
        return cache.entries
            .filter { it.key.substringAfterLast('.').startsWith(prefix) }
            .associate { entry ->
                val qualifiedName = entry.value.qualifiedName()
                entry.key.substringBeforeLast('.') to qualifiedName
            }
    }

    fun getClasses(): MutableSet<MutableMap.MutableEntry<String, CtClass>> {
        return cache.entries
    }

    fun getPackages(): MutableSet<MutableMap.MutableEntry<String, String>> {
        return packageCache.entries
    }
}

fun CtClass.qualifiedName(): String {
    // name is in this format: com.example.ClassName$InnerClass.class
    return name.substringAfterLast('.').replace('$', '.')
}
