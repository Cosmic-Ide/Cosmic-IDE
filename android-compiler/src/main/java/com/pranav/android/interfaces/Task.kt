package com.pranav.android.interfaces;

interface class Task {

    fun getTaskName() : String

    @Throws(Exception::class)
    fun doFullTask()
}
