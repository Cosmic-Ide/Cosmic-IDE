package com.pranav.android.interfaces

import com.pranav.project.mode.JavaProject

interface Task {

    fun getTaskName() : String

    @Throws(Exception::class)
    fun doFullTask(project: JavaProject)

}