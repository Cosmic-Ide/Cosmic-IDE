package com.pranav.android.interfaces;

interface Task {

    fun getTaskName() : String

    @Throws(Exception::class)
    fun doFullTask()
}
