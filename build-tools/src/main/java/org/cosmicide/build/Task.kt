package org.cosmicide.build

/**
 * A Task represents a unit of work that can be executed.
 */
interface Task {
    
    /**
     * Executes the task and reports any progress or errors to the given [reporter].
     *
     * @param reporter the reporter to which progress or errors should be reported
     */
    fun execute(reporter: BuildReporter)
}