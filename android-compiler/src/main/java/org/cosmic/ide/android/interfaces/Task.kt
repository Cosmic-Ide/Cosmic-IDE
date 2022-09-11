package org.cosmic.ide.android.interfaces

import org.cosmic.ide.project.Project

interface Task {

    fun getTaskName(): String

    @Throws(Exception::class)
    fun doFullTask(project: Project)
}
