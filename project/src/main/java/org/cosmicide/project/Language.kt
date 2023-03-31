package org.cosmicide.project

import org.cosmicide.project.templates.javaClass
import org.cosmicide.project.templates.kotlinClass
import java.io.Serializable

/**
 * A sealed class representing a programming language.
 *
 * @property extension the file extension associated with the language
 */
sealed class Language(val extension: String) : Serializable {

    /**
     * Generates the content of a class file for the language.
     *
     * @param name the name of the class
     * @param packageName the name of the package the class belongs to
     * @return the generated class file content as a string
     */
    abstract fun classFileContent(name: String, packageName: String): String

    /**
     * An object representing the Java programming language.
     */
    object Java : Language("java") {
        override fun classFileContent(name: String, packageName: String): String {
            return javaClass(name, packageName, """
                System.out.println("Hello, World!");
            """.trimIndent())
        }
    }

    /**
     * An object representing the Kotlin programming language.
     */
    object Kotlin : Language("kt") {
        override fun classFileContent(name: String, packageName: String): String {
            return kotlinClass(name, packageName, """
                println("Hello World!")
            """.trimIndent())
        }
    }
}

/**
 * Returns an instance of [Language] with the specified file extension.
 *
 * @param extension the file extension of the language to create
 * @return an instance of [Language]
 * @throws IllegalArgumentException if the specified extension is not supported
 */
fun language(extension: String): Language {
    return when (extension) {
        "java" -> Language.Java
        "kt" -> Language.Kotlin
        else -> throw IllegalArgumentException("Unsupported extension: $extension")
    }
}