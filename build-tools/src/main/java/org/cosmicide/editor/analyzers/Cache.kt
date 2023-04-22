package org.cosmicide.editor.analyzers

import org.cosmicide.build.dex.D8Task
import org.cosmicide.build.java.JavaCompileTask
import org.cosmicide.build.kotlin.KotlinCompiler
import java.io.File
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

/**
 * A data class that holds instances of [JavaCompileTask], [KotlinCompiler], and [D8Task].
 * Used to cache these instances for efficient re-use.
 *
 * @property javaCompiler an instance of [JavaCompileTask]
 * @property kotlinCompiler an instance of [KotlinCompiler]
 * @property dexTask an instance of [D8Task]
 */
object Cache {

    val cacheMap = mutableMapOf<String, JavaFileObject>()
    fun saveCache(file: File): JavaFileObject {
        val obj = object : SimpleJavaFileObject(file.toURI(), JavaFileObject.Kind.SOURCE) {
            private val lastModified = file.lastModified()
            override fun getCharContent(ignoreEncodingErrors: Boolean): CharSequence {
                return file.readText()
            }

            override fun getLastModified(): Long {
                return lastModified
            }
        }
        cacheMap[file.absolutePath] = obj
        return obj
    }

    fun saveCache(obj: JavaFileObject) {
        cacheMap[obj.name] = obj
    }

    fun getCache(key: File): JavaFileObject? {
        return cacheMap[key.absolutePath]
    }
}