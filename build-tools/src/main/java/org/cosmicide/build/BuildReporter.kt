package org.cosmicide.build

/**
 * Enum to represent different kinds of build reports.
 */
enum class KIND {
    INFO,
    WARNING,
    ERROR,
    LOGGING,
    OUTPUT
}

/**
 * Data class to represent a build report.
 * @property kind The kind of build report.
 * @property message The message associated with the build report.
 */
data class BuildReport(val kind: KIND, val message: String)

/**
 * Class for reporting build progress.
 * @property callback A function that takes a [BuildReport] as input and handles it.
 */
class BuildReporter(
    val callback: (BuildReport) -> Unit = { report -> println("${report.kind}: ${report.message}") }
) {
    var buildSuccess = false

    /**
     * Generates an informational build report.
     * @param message The message associated with the build report.
     */
    fun reportInfo(message: String) {
        callback(BuildReport(KIND.INFO, message))
    }

    /**
     * Generates a warning build report.
     * @param message The message associated with the build report.
     */
    fun reportWarning(message: String) {
        callback(BuildReport(KIND.WARNING, message))
    }

    /**
     * Generates an error build report.
     * @param message The message associated with the build report.
     */
    fun reportError(message: String) {
        callback(BuildReport(KIND.ERROR, message))
    }

    /**
     * Generates a logging build report.
     * @param message The message associated with the build report.
     */
    fun reportLogging(message: String) {
        callback(BuildReport(KIND.LOGGING, message))
    }

    /**
     * Generates an output build report.
     * @param message The message associated with the build report.
     */
    fun reportOutput(message: String) {
        callback(BuildReport(KIND.OUTPUT, message))
    }

    /**
     * Generates a success build report.
     * Reports that the build completed successfully.
     */
    fun reportSuccess() {
        reportOutput("Build completed successfully.")
        buildSuccess = true
    }
}