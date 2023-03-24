package org.cosmicide.rewrite.util

import org.cosmicide.project.Project

class ProjectHandler {
    companion object {
        var project: Project? = null
        private var editorFragmentListener: (Boolean) -> Unit = {}

        fun setEditorFragmentListener(listener: (Boolean) -> Unit) {
            editorFragmentListener = listener
        }

        fun onEditorFragmentChange(opened: Boolean) {
            editorFragmentListener(opened)
        }
    }
}