/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.project

import java.io.Serializable

/**
 * A sealed class representing a programming language.
 *
 * @property extension the file extension associated with the language
 */
@kotlinx.serialization.Serializable
sealed class Language(val extension: String) : Serializable {

    abstract val name: String

    /**
     * An object representing the Java programming language.
     */
    @kotlinx.serialization.Serializable
    object Java : Language("java") {
        override val name = "Java"
    }

    /**
     * An object representing the Kotlin programming language.
     */
    @kotlinx.serialization.Serializable
    object Kotlin : Language("kt") {
        override val name = "Kotlin"
    }

    /**
     * An object representing the Scala programming language.
     */
    @kotlinx.serialization.Serializable
    object Scala : Language("scala") {
        override val name = "Scala"
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
        "scala" -> Language.Scala
        else -> throw IllegalArgumentException("Unsupported extension: $extension")
    }
}
