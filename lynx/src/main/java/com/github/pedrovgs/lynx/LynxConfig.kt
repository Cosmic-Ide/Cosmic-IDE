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
package com.github.pedrovgs.lynx

import com.github.pedrovgs.lynx.model.TraceLevel
import java.io.Serializable

/**
 * Lynx configuration parameters used to open main activity. All the configuration library is
 * provided by library clients using this class. With LynxConfig you can privde different values
 * for:
 *
 *
 * - Max number of traces to show in LynxView. - Filter used to get a list of traces to show. -
 * Text size in DP used to render a trace. - Sampling rate used to read from the Logcat output.
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
class LynxConfig : Serializable, Cloneable {
    var maxNumberOfTracesToShow = 2500
        private set
    var filter: String? = ""
        private set
    var filterTraceLevel: TraceLevel
        private set
    var textSizeInPx: Float? = null
    var samplingRate = 150

    init {
        filterTraceLevel = TraceLevel.VERBOSE
    }

    fun setMaxNumberOfTracesToShow(maxNumberOfTracesToShow: Int): LynxConfig {
        require(maxNumberOfTracesToShow > 0) { "You can't use a max number of traces equals or lower than zero." }
        this.maxNumberOfTracesToShow = maxNumberOfTracesToShow
        return this
    }

    fun setFilter(filter: String?): LynxConfig {
        requireNotNull(filter) { "filter can't be null" }
        this.filter = filter
        return this
    }

    fun setFilterTraceLevel(filterTraceLevel: TraceLevel?): LynxConfig {
        requireNotNull(filterTraceLevel) { "filterTraceLevel can't be null" }
        this.filterTraceLevel = filterTraceLevel
        return this
    }

    private fun setSamplingRate(samplingRate: Int): LynxConfig {
        this.samplingRate = samplingRate
        return this
    }

    fun hasFilter(): Boolean {
        return "" != filter || TraceLevel.VERBOSE != filterTraceLevel
    }

    fun getTextSizeInPx(): Float {
        return if (textSizeInPx == null) DEFAULT_TEXT_SIZE_IN_PX else textSizeInPx!!
    }

    fun hasTextSizeInPx(): Boolean {
        return textSizeInPx != null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (
            other !is LynxConfig
            || maxNumberOfTracesToShow != other.maxNumberOfTracesToShow
            || samplingRate != other.samplingRate
            || filter != other.filter
            || textSizeInPx != other.textSizeInPx
        ) return false
        return filterTraceLevel == other.filterTraceLevel
    }

    override fun hashCode(): Int {
        var result = maxNumberOfTracesToShow
        result = 31 * result + if (filter != null) filter.hashCode() else 0
        result = 31 * result + if (textSizeInPx != null) textSizeInPx.hashCode() else 0
        result = 31 * result + samplingRate
        return result
    }

    public override fun clone(): Any {
        return LynxConfig()
            .setMaxNumberOfTracesToShow(maxNumberOfTracesToShow)
            .setFilter(filter)
            .setFilterTraceLevel(filterTraceLevel)
            .setSamplingRate(samplingRate)
    }

    companion object {
        private const val serialVersionUID = 293939299388293L
        private const val DEFAULT_TEXT_SIZE_IN_PX = 36f
    }
}