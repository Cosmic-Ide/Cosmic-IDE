package org.cosmicide.rewrite.util

import org.cosmicide.project.Project

/**
 * A utility class for handling the current project.
 */
class ProjectHandler {

    companion object {
        private var project: Project? = null

        /**
         * Gets the current project.
         * @return the current project, or null if no project is set
         */
        fun getProject(): Project? {
            return project
        }

        /**
         * Sets the current project.
         * @param project the project to set
         */
        fun setProject(project: Project) {
            this.project = project
        }
    }
}