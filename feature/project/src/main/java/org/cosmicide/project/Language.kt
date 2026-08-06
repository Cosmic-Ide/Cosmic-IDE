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

    @kotlinx.serialization.Serializable
    object Empty : Language("txt") {
        override val name = "Empty"
    }

    /**
     * A language supplied by an installed plugin.
     *
     * Keeping this carrier in the stable project model lets plugins add project types without
     * requiring every language to be compiled into Cosmic itself.
     */
    @kotlinx.serialization.Serializable
    data class Custom(
        private val displayName: String,
        private val sourceExtension: String
    ) : Language(sourceExtension) {
        init {
            require(displayName.isNotBlank()) { "Language name must not be blank" }
            require(sourceExtension.matches(Regex("[A-Za-z0-9][A-Za-z0-9_+-]*"))) {
                "Language extension must be a file extension without a leading dot"
            }
        }

        override val name: String = displayName
    }
}
