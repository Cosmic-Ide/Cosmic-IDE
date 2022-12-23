/*
 * Copyright (C) 2015 Pedro Vicente Gomez Sanchez.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.pedrovgs.lynx.model

import com.github.pedrovgs.lynx.exception.IllegalTraceException

/**
 * Logcat trace representation. All traces contains a message and a TraceLevel assigned.
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
class Trace(val level: TraceLevel, val message: String) {

    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }
        if (other !is Trace) {
            return false
        }
        return level == other.level && message == other.message
    }

    override fun hashCode(): Int {
        var result = level.hashCode()
        result = 31 * result + message.hashCode()
        return result
    }
    companion object {
        private const val TRACE_LEVEL_SEPARATOR = '/'
        private const val START_OF_MESSAGE_INDEX = 2
        const val MIN_TRACE_SIZE = 2
        const val TRACE_LEVEL_INDEX = 0

        /**
         * Factory method used to create a Trace instance from a String. The format of the input string
         * have to be something like: "02-07 17:45:33.014 D/Any debug trace"
         *
         * @param logcatTrace the logcat string
         * @return a new Trace instance
         * @throws IllegalTraceException if the string argument is an invalid string
         */
        @Throws(IllegalTraceException::class)
        fun fromString(logcatTrace: String?): Trace {
            if (logcatTrace == null || logcatTrace.length < MIN_TRACE_SIZE || logcatTrace[1] != TRACE_LEVEL_SEPARATOR) {
                throw IllegalTraceException(
                    "You are trying to create a Trace object from a invalid String. Your trace have"
                            + " to be something like: 'D/Any debug trace'."
                )
            }
            val level = TraceLevel.getTraceLevel(logcatTrace[TRACE_LEVEL_INDEX])
            val message = logcatTrace.substring(START_OF_MESSAGE_INDEX)
            return Trace(level, message)
        }
    }
}