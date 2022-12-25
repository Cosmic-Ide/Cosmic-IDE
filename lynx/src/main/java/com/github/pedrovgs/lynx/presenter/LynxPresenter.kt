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

import com.github.pedrovgs.lynx.LynxConfig
import com.github.pedrovgs.lynx.model.Lynx
import com.github.pedrovgs.lynx.model.Trace
import com.github.pedrovgs.lynx.model.TraceLevel

/**
 * Presenter created to decouple Lynx library view implementations from Lynx model. This presenter
 * responsibility is related to all the presentation logic to Lynx UI implementations. Lynx UI
 * implementations have to implement LynxPresenter.View interface.
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
class LynxPresenter(lynx: Lynx, view: View, maxNumberOfTracesToShow: Int) : Lynx.Listener {
    private val lynx: Lynx
    private val view: View
    private val traceBuffer: TraceBuffer
    private var isInitialized = false

    init {
        validateNumberOfTracesConfiguration(maxNumberOfTracesToShow.toLong())
        this.lynx = lynx
        this.view = view
        traceBuffer = TraceBuffer(maxNumberOfTracesToShow)
    }

    /**
     * Updates and applies a new lynx configuration based on the LynxConfig object passed as
     * parameter.
     *
     * @param lynxConfig the lynx configuration
     */
    fun setLynxConfig(lynxConfig: LynxConfig) {
        validateLynxConfig(lynxConfig)
        updateBufferConfig(lynxConfig)
        updateLynxConfig(lynxConfig)
    }

    /** Initializes presenter lifecycle if it wasn't initialized before.  */
    fun resume() {
        if (!isInitialized) {
            isInitialized = true
            lynx.registerListener(this)
            lynx.startReading()
        }
    }

    /** Stops presenter lifecycle if it was previously initialized.  */
    fun pause() {
        if (isInitialized) {
            isInitialized = false
            lynx.stopReading()
            lynx.unregisterListener(this)
        }
    }

    /** Given a list of Trace objects to show, updates the buffer of traces and refresh the view.  */
    override fun onNewTraces(traces: List<Trace>?) {
        val tracesRemoved = updateTraceBuffer(traces)
        val tracesToNotify = currentTraces
        view.showTraces(tracesToNotify, tracesRemoved)
    }

    /**
     * Updates the filter used to know which Trace objects we have to show in the UI.
     *
     * @param filter the filter to use
     */
    fun updateFilter(filter: String?) {
        if (isInitialized) {
            val lynxConfig = lynx.config
            lynxConfig.setFilter(filter)
            lynx.config = lynxConfig
            clearView()
            restartLynx()
        }
    }

    fun updateFilterTraceLevel(level: TraceLevel?) {
        if (isInitialized) {
            clearView()
            val lynxConfig = lynx.config
            lynxConfig.setFilterTraceLevel(level)
            lynx.config = lynxConfig
            restartLynx()
        }
    }

    /**
     * Based on the int passed as parameter changes auto scroll feature configuration to
     * enable/disabled. If the last visible item of the list is in the last position of the list,
     * enables auto scroll, if not, disables auto scroll.
     *
     * @param lastVisiblePositionInTheList the index of the last visible position
     */
    fun onScrollToPosition(lastVisiblePositionInTheList: Int) {
        if (shouldDisableAutoScroll(lastVisiblePositionInTheList)) {
            view.disableAutoScroll()
        } else {
            view.enableAutoScroll()
        }
    }

    /**
     * Returns a list of the current traces stored in this presenter.
     *
     * @return a list of the current traces
     */
    val currentTraces: List<Trace>
        get() = traceBuffer.traces

    private fun clearView() {
        traceBuffer.clear()
        view.clear()
    }

    private fun restartLynx() {
        lynx.restart()
    }

    private fun updateBufferConfig(lynxConfig: LynxConfig) {
        traceBuffer.setBufferSize(lynxConfig.maxNumberOfTracesToShow)
        refreshTraces()
    }

    private fun refreshTraces() {
        onNewTraces(traceBuffer.traces)
    }

    private fun updateLynxConfig(lynxConfig: LynxConfig) {
        lynx.config = lynxConfig
    }

    private fun updateTraceBuffer(traces: List<Trace>?): Int {
        return traceBuffer.add(traces)
    }

    private fun validateNumberOfTracesConfiguration(maxNumberOfTracesToShow: Long) {
        require(maxNumberOfTracesToShow > 0) { "You can't pass a zero or negative number of traces to show." }
    }

    private fun validateLynxConfig(lynxConfig: LynxConfig?) {
        requireNotNull(lynxConfig) { "You can't use a null instance of LynxConfig as configuration." }
    }

    private fun shouldDisableAutoScroll(lastVisiblePosition: Int): Boolean {
        val positionOffset = traceBuffer.currentNumberOfTraces - lastVisiblePosition
        return positionOffset >= MIN_VISIBLE_POSITION_TO_ENABLE_AUTO_SCROLL
    }

    interface View {
        fun showTraces(traces: List<Trace>?, removedTraces: Int)
        fun clear()

        fun notifyShareTracesFailed()
        fun disableAutoScroll()
        fun enableAutoScroll()
    }

    companion object {
        private const val MIN_VISIBLE_POSITION_TO_ENABLE_AUTO_SCROLL = 3
    }
}