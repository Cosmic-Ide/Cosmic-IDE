/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

import org.cosmicide.project.Project

/**
 * A utility class for handling the current project.
 */
class ProjectHandler {

    companion object {
        /**
         * The current project.
         */
        @JvmStatic
        private var project: Project? = null

        var clazz: String? = null

        /**
         * Gets the current project.
         * @return the current project, or null if no project is set
         */
        @JvmStatic
        fun getProject(): Project? {
            return project
        }

        /**
         * Sets the current project.
         * @param project the project to set
         */
        @JvmStatic
        fun setProject(project: Project) {
            Companion.project = project
        }
    }
}
