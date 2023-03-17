package org.cosmicide.build

interface Task {
    fun execute(reporter: BuildReporter)
}