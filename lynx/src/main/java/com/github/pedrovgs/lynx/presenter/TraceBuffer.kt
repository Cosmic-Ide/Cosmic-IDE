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
package com.github.pedrovgs.lynx.presenter

import com.github.pedrovgs.lynx.model.Trace
import java.util.LinkedList

/**
 * Buffer created to keep a max number of traces and be able to configure the size of the buffer.
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
internal class TraceBuffer(private var bufferSize: Int) {
    val traces: MutableList<Trace>

    init {
        traces = LinkedList()
    }

    /** Configures the max number of traces to keep inside the buffer  */
    fun setBufferSize(bufferSize: Int) {
        this.bufferSize = bufferSize
        removeExceededTracesIfNeeded()
    }

    /**
     * Adds a list of traces to the buffer, if the buffer is full your new traces will be added and
     * the previous one will be removed.
     */
    fun add(traces: List<Trace>?): Int {
        this.traces.addAll(traces!!)
        return removeExceededTracesIfNeeded()
    }

    /** Returns the number of traces stored in the buffer.  */
    val currentNumberOfTraces: Int
        get() = traces.size

    /** Removes traces stored in the buffer.  */
    fun clear() {
        traces.clear()
    }

    private fun removeExceededTracesIfNeeded(): Int {
        val tracesToDiscard = numberOfTracesToDiscard
        if (tracesToDiscard > 0) {
            discardTraces(tracesToDiscard)
        }
        return tracesToDiscard
    }

    private val numberOfTracesToDiscard: Int
        get() {
            val currentTracesSize = traces.size
            var tracesToDiscard = currentTracesSize - bufferSize
            tracesToDiscard = if (tracesToDiscard < 0) 0 else tracesToDiscard
            return tracesToDiscard
        }

    private fun discardTraces(tracesToDiscard: Int) {
        for (i in 0 until tracesToDiscard) {
            traces.removeAt(0)
        }
    }
}