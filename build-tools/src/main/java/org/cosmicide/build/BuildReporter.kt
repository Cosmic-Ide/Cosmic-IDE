package org.cosmicide.build

sealed interface KIND {
    object INFO : KIND {
        override fun toString(): String {
            return "INFO"
        }
    }

    object WARNING : KIND {
        override fun toString(): String {
            return "WARNING"
        }
    }

    object ERROR : KIND {
        override fun toString(): String {
            return "ERROR"
        }
    }

    object LOGGING : KIND {
        override fun toString(): String {
            return "LOGGING"
        }
    }

    object OUTPUT : KIND {
        override fun toString(): String {
            return "OUTPUT"
        }
    }
}

class BuildReporter(val callback: (KIND, String) -> Unit) {
    fun reportInfo(message: String) {
        callback(KIND.INFO, message)
    }

    fun reportWarning(message: String) {
        callback(KIND.WARNING, message)
    }

    fun reportError(message: String) {
        callback(KIND.ERROR, message)
    }

    fun reportLogging(message: String) {
        callback(KIND.LOGGING, message)
    }

    fun reportOutput(message: String) {
        callback(KIND.OUTPUT, message)
    }
}