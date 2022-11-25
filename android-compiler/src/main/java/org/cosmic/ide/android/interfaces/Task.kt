package org.cosmic.ide.android.interfaces

import org.cosmic.ide.project.Project

interface Task {

    @Throws(Exception::class)
    fun doFullTask(project: Project)
}
