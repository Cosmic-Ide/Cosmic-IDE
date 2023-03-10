package org.cosmicide.project

/**
 * A sealed interface representing a programming language.
 *
 * @property extension the file extension associated with the language
 */
sealed interface Language {
    val extension: String

    /**
     * Generates the content of a class file for the language.
     *
     * @param name the name of the class
     * @param packageName the name of the package the class belongs to
     * @return the generated class file content as a string
     */
    fun classFileContent(name: String, packageName: String): String
}

/**
 * An object representing the Java programming language.
 */
object Java : Language {
    override val extension = "java"

    /**
     * Generates the content of a Java class file.
     *
     * @param name the name of the class
     * @param packageName the name of the package the class belongs to
     * @return the generated Java class file content as a string
     */
    override fun classFileContent(name: String, packageName: String): String {
        return Templates.javaClass(
            name, packageName, """
            System.out.println("Hello, World!");
        """.trimIndent()
        )
    }
}

/**
 * An object representing the Kotlin programming language.
 */
object Kotlin : Language {
    override val extension = "kt"

    /**
     * Generates the content of a Kotlin class file.
     *
     * @param name the name of the class
     * @param packageName the name of the package the class belongs to
     * @return the generated Kotlin class file content as a string
     */
    override fun classFileContent(name: String, packageName: String): String {
        return Templates.kotlinClass(
            name, packageName, """
            println("Hello World!")
        """.trimIndent()
        )
    }
}
