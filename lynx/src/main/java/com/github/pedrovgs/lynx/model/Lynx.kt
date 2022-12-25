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

import android.util.Log
import com.github.pedrovgs.lynx.LynxConfig
import com.github.pedrovgs.lynx.exception.IllegalTraceException
import java.util.LinkedList
import java.util.Locale
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

/**
 * Main business logic class for this project. Lynx responsibility is related to listen Logcat
 * events and notify it to the Lynx listeners transforming all the information from a plain String
 * to a Trace with all the information needed.
 *
 *
 * Given a LynxConfig object the sample rating used to notify Lynx clients about new traces can
 * be modified on demand. LynxConfig object will be used to filter traces if any filter has been
 * previously configured. Filtering will remove traces that contains given string or that match a
 * regular expression specified as filter.
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
class Lynx(logcat: Logcat, mainThread: MainThread, timeProvider: TimeProvider) {
    private var logcat: Logcat
    private val mainThread: MainThread
    private val timeProvider: TimeProvider
    private val tracesToNotify: MutableList<Trace>
    private val listeners: MutableList<Listener>
    private var lynxConfig = LynxConfig()
    private var lastNotificationTime: Long = 0
    private var lowerCaseFilter = ""
    private var regexpFilter: Pattern? = null

    init {
        listeners = LinkedList()
        tracesToNotify = LinkedList()
        this.logcat = logcat
        this.mainThread = mainThread
        this.timeProvider = timeProvider
        setFilters()
    }

    /**
     * Indicates a custom LynxConfig object.
     */
    @set:Synchronized
    var config: LynxConfig
        get() = lynxConfig.clone() as LynxConfig
        set(lynxConfig) {
            this.lynxConfig = lynxConfig
            setFilters()
        }

    /**
     * Configures a Logcat.Listener and initialize Logcat dependency to read traces from the OS log.
     */
    fun startReading() {
        logcat.listener = object : Logcat.Listener {

            override fun onTraceRead(logcatTrace: String?) {
                try {
                    addTraceToTheBuffer(logcatTrace!!)
                } catch (e: IllegalTraceException) {
                    return
                }
                notifyNewTraces()            }
        }
        val logcatWasNotStarted = Thread.State.NEW == logcat.state
        if (logcatWasNotStarted) {
            logcat.start()
        }
    }

    /** Stops Logcat dependency to stop receiving logcat traces.  */
    fun stopReading() {
        logcat.stopReading()
        logcat.interrupt()
    }

    /**
     * Stops the configured Logcat dependency and creates a clone to restart using Logcat and
     * LogcatListener configured previously.
     */
    @Synchronized
    fun restart() {
        val previousListener = logcat.listener
        logcat.stopReading()
        logcat.interrupt()
        logcat = logcat.clone() as Logcat
        logcat.listener = previousListener
        lastNotificationTime = 0
        tracesToNotify.clear()
        logcat.start()
    }

    /**
     * Adds a Listener to the listeners collection to be notified with new Trace objects.
     *
     * @param lynxPresenter a lynx listener
     */
    @Synchronized
    fun registerListener(lynxPresenter: Listener) {
        listeners.add(lynxPresenter)
    }

    /**
     * Removes a Listener to the listeners collection.
     *
     * @param lynxPresenter a lynx listener
     */
    @Synchronized
    fun unregisterListener(lynxPresenter: Listener) {
        listeners.remove(lynxPresenter)
    }

    private fun setFilters() {
        lowerCaseFilter = lynxConfig.filter!!.lowercase(Locale.getDefault())
        try {
            regexpFilter = Pattern.compile(lowerCaseFilter)
        } catch (exception: PatternSyntaxException) {
            regexpFilter = null
            Log.d(LOGTAG, "Invalid regexp filter!")
        }
    }

    @Synchronized
    @Throws(IllegalTraceException::class)
    private fun addTraceToTheBuffer(logcatTrace: String) {
        if (shouldAddTrace(logcatTrace)) {
            val trace = Trace.fromString(logcatTrace)
            tracesToNotify.add(trace)
        }
    }

    private fun shouldAddTrace(logcatTrace: String): Boolean {
        val hasFilterConfigured = lynxConfig.hasFilter()
        val hasMinSize = logcatTrace.length >= Trace.MIN_TRACE_SIZE
        return hasMinSize && (!hasFilterConfigured || traceMatchesFilter(logcatTrace))
    }

    @Synchronized
    private fun traceMatchesFilter(logcatTrace: String): Boolean {
        return (traceStringMatchesFilter(logcatTrace)
                && containsTraceLevel(logcatTrace, lynxConfig.filterTraceLevel))
    }

    private fun traceStringMatchesFilter(logcatTrace: String): Boolean {
        val lowerCaseLogcatTrace = logcatTrace.lowercase(Locale.getDefault())
        var matchesFilter = lowerCaseLogcatTrace.contains(lowerCaseFilter)
        if (!matchesFilter && regexpFilter != null) {
            matchesFilter = regexpFilter!!.matcher(lowerCaseLogcatTrace).find()
        }
        return matchesFilter
    }

    private fun containsTraceLevel(logcatTrace: String, levelFilter: TraceLevel): Boolean {
        return levelFilter == TraceLevel.VERBOSE || hasTraceLevelEqualOrHigher(
            logcatTrace,
            levelFilter
        )
    }

    private fun hasTraceLevelEqualOrHigher(logcatTrace: String, levelFilter: TraceLevel): Boolean {
        val level = TraceLevel.getTraceLevel(logcatTrace[Trace.TRACE_LEVEL_INDEX])
        return level.ordinal >= levelFilter.ordinal
    }

    @Synchronized
    private fun notifyNewTraces() {
        if (shouldNotifyListeners()) {
            val traces: List<Trace> = LinkedList(tracesToNotify)
            tracesToNotify.clear()
            notifyListeners(traces)
        }
    }

    @Synchronized
    private fun shouldNotifyListeners(): Boolean {
        val now = timeProvider.currentTimeMillis
        val timeFromLastNotification = now - lastNotificationTime
        val hasTracesToNotify = tracesToNotify.size > 0
        return timeFromLastNotification > lynxConfig.samplingRate && hasTracesToNotify
    }

    @Synchronized
    private fun notifyListeners(traces: List<Trace>) {
        mainThread.post {
            for (listener in listeners) {
                listener.onNewTraces(traces)
            }
            lastNotificationTime = timeProvider.currentTimeMillis
        }
    }

    interface Listener {
        fun onNewTraces(traces: List<Trace>?)
    }

    companion object {
        private const val LOGTAG = "Lynx"
    }
}