package org.cosmic.ide.android.interfaces

import org.cosmic.ide.project.JavaProject

interface Task {

    fun getTaskName() : String

    @Throws(Exception::class)
    fun doFullTask(project: JavaProject)

}