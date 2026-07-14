/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.project

import kotlinx.serialization.KSerializer
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Represents a project.
 *
 * @property root The root directory of the project.
 * @property language The programming language used in the project.
 */
@Serializable
data class Project(
    @Serializable(with = FileSerializer::class) val root: File,
    val language: Language
): java.io.Serializable {

    /**
     * The name of the project, derived from the root directory.
     */
    val name: String = root.name

    /**
     * The source directory of the project, based on the language used.
     */
    val srcDir: File
        get() {
            val languageDirectory = when (language) {
                is Language.Java -> "java"
                is Language.Kotlin -> "kotlin"
                is Language.Scala -> "scala"
            }
            val sourceSet = root.resolve("src/main/$languageDirectory")
            val applicationSourceSet = root.resolve("app/src/main/$languageDirectory")
            return applicationSourceSet.takeIf { it.isDirectory } ?: sourceSet
        }

    /**
     * The build directory of the project.
     */
    @Serializable(with = FileSerializer::class)
    val buildDir = File(root, "build")

    /**
     * The cache directory of the project.
     */
    @Serializable(with = FileSerializer::class)
    val cacheDir = File(buildDir, "cache")

    /**
     * The binary directory of the project.
     */
    @Serializable(with = FileSerializer::class)
    val binDir = File(buildDir, "bin")

    /**
     * The library directory of the project.
     */
    @Serializable(with = FileSerializer::class)
    val libDir = File(root, "libs")

    var runtimeArgs = listOf<String>()
        get() {
            val f = cacheDir.resolve("jre.txt")
            if (f.exists()) {
                return f.readLines().toMutableList()
            }

            return listOf()
        }
        set(value) {
            val f = cacheDir.resolve("jre.txt")
            f.parentFile.mkdirs()
            f.writeText(value.joinToString("\n"))
            field = value
        }

    var args = listOf<String>()
        get() {
            val f = cacheDir.resolve("args.txt")
            if (f.exists()) {
                return f.readLines().toMutableList()
            }

            return listOf()
        }
        set(value) {
            val f = cacheDir.resolve("args.txt")
            f.parentFile.mkdirs()
            f.writeText(value.joinToString("\n"))
            field = value
        }

    /**
     * Deletes the project directory.
     *
     * @throws IllegalStateException if the root directory is not a valid project directory.
     */
    fun delete() {
        if (root.isDirectory && root.name == name) {
            root.deleteRecursively()
        } else {
            throw IllegalStateException("Cannot delete directory: ${root.absolutePath}")
        }
    }
}

object FileSerializer : KSerializer<File> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.io.File", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: File) {
        encoder.encodeString(value.absolutePath)
    }

    override fun deserialize(decoder: Decoder): File {
        return File(decoder.decodeString())
    }
}
