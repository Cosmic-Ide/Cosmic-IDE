package com.pranav.android.interfaces;

public abstract class Task {

    abstract fum getTaskName() : String

    @Throws(Exception::class)
    abstract fun doFullTask()
}
