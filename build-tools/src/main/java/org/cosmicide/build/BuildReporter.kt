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
}