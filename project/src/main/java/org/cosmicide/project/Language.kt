package org.cosmicide.project

sealed interface Language {
    val extension: String
    fun getClassFile(name: String, packageName: String): String
}

object Java : Language {
    override val extension = "java"
    override fun getClassFile(name: String, packageName: String): String {
        return Templates.javaClass(
            name, packageName, """
            System.out.println("Hello, World!");
        """.trimIndent()
        )
    }
}

object Kotlin : Language {
    override val extension = "kt"
    override fun getClassFile(name: String, packageName: String): String {
        return Templates.kotlinClass(
            name, packageName, """
            println("Hello World!")
        """.trimIndent()
        )
    }
}
