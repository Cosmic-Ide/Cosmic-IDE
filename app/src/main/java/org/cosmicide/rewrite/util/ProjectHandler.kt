package org.cosmicide.rewrite.util

import org.cosmicide.project.Project

/**
 * A utility class for handling the current project and editor fragment listener.
 */
class ProjectHandler {

    companion object {
        private var project: Project? = null
        private var editorFragmentListener: ((Boolean) -> Unit)? = null

        /**
         * Sets the listener for editor fragment changes.
         * @param listener the listener to set
         */
        fun setEditorFragmentListener(listener: (Boolean) -> Unit) {
            editorFragmentListener = listener
        }

        /**
         * Notifies the editor fragment listener of a change in the editor fragment state.
         * @param opened true if the editor fragment is open, false otherwise
         */
        fun onEditorFragmentChange(opened: Boolean) {
            editorFragmentListener?.invoke(opened)
        }

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