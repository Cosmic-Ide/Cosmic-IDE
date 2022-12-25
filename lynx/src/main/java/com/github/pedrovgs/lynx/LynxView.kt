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

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.github.pedrovgs.lynx.model.AndroidMainThread
import com.github.pedrovgs.lynx.model.Logcat
import com.github.pedrovgs.lynx.model.Lynx
import com.github.pedrovgs.lynx.model.TimeProvider
import com.github.pedrovgs.lynx.model.Trace
import com.github.pedrovgs.lynx.model.TraceLevel
import com.github.pedrovgs.lynx.presenter.LynxPresenter
import com.github.pedrovgs.lynx.renderer.TraceRendererBuilder
import com.pedrogomez.renderers.RendererAdapter
import com.pedrogomez.renderers.RendererBuilder

/**
 * Main library view. Custom view based on a RelativeLayout used to show all the information printed
 * by the Android Logcat. Add this view to your layouts if you want to show your Logcat traces and
 * configure it using styleable attributes.
 *
 * @author Pedro Vicente Gomez Sanchez.
 */
class LynxView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr), LynxPresenter.View {
    private var presenter: LynxPresenter? = null

    /**
     * Returns the current LynxConfig object used.
     *
     * @return the lynx configuration
     */
    var lynxConfig: LynxConfig? = null
        private set
    private var lvTraces: ListView? = null
    private var etFilter: EditText? = null
    private var spFilter: Spinner? = null
    private var adapter: RendererAdapter<Trace>? = null
    private var lastScrollPosition = 0

    init {
        initializeConfiguration(attrs)
        initializePresenter()
        initializeView()
    }

    /** Initializes LynxPresenter if LynxView is visible when is attached to the window.  */
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (isVisible) {
            resumePresenter()
        }
    }

    /** Stops LynxPresenter when LynxView is detached from the window.  */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pausePresenter()
    }

    /**
     * Initializes or stops LynxPresenter based on visibility changes. Doing this Lynx is not going
     * to read your application Logcat if LynxView is not visible or attached.
     */
    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        if (changedView !== this) {
            return
        }
        if (visibility == VISIBLE) {
            resumePresenter()
        } else {
            pausePresenter()
        }
    }

    /**
     * Given a valid LynxConfig object update all the dependencies to apply this new configuration.
     *
     * @param lynxConfig the lynx configuration
     */
    fun setLynxConfig(lynxConfig: LynxConfig) {
        validateLynxConfig(lynxConfig)
        val hasChangedLynxConfig = this.lynxConfig != lynxConfig
        if (hasChangedLynxConfig) {
            this.lynxConfig = lynxConfig.clone() as LynxConfig
            updateFilterText()
            updateAdapter()
            updateSpinner()
            presenter!!.setLynxConfig(lynxConfig)
        }
    }

    private fun updateSpinner() {
        val filterTraceLevel = lynxConfig!!.filterTraceLevel
        spFilter!!.setSelection(filterTraceLevel.ordinal)
    }

    /**
     * Given a `List<Trace>` updates the ListView adapter with this information and keeps the
     * scroll position if needed.
     */
    override fun showTraces(traces: List<Trace>?, removedTraces: Int) {
        if (lastScrollPosition == 0) {
            lastScrollPosition = lvTraces!!.firstVisiblePosition
        }
        adapter!!.clear()
        adapter!!.addAll(traces)
        adapter!!.notifyDataSetChanged()
        updateScrollPosition(removedTraces)
    }

    /** Removes all the traces rendered in the ListView.  */
    override fun clear() {
        adapter!!.clear()
        adapter!!.notifyDataSetChanged()
    }

    override fun notifyShareTracesFailed() {
        Toast.makeText(context, "Share failed", Toast.LENGTH_SHORT).show()
    }

    override fun disableAutoScroll() {
        lvTraces!!.transcriptMode = AbsListView.TRANSCRIPT_MODE_DISABLED
    }

    override fun enableAutoScroll() {
        lvTraces!!.transcriptMode = AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
    }

    private val isPresenterReady: Boolean
        get() = presenter != null

    private fun resumePresenter() {
        if (isPresenterReady) {
            presenter!!.resume()
            val lastPosition = adapter!!.count - 1
            lvTraces!!.setSelection(lastPosition)
        }
    }

    private fun pausePresenter() {
        if (isPresenterReady) {
            presenter!!.pause()
        }
    }

    private val isVisible: Boolean
        get() = visibility == VISIBLE

    @SuppressLint("CustomViewStyleable")
    private fun initializeConfiguration(attrs: AttributeSet?) {
        lynxConfig = LynxConfig()
        if (attrs != null) {
            val attributes = context.obtainStyledAttributes(attrs, R.styleable.lynx)
            val maxTracesToShow = attributes.getInteger(
                R.styleable.lynx_max_traces_to_show,
                lynxConfig!!.maxNumberOfTracesToShow
            )
            val filter = attributes.getString(R.styleable.lynx_filter)
            var fontSizeInPx = attributes.getDimension(R.styleable.lynx_text_size, -1f)
            if (fontSizeInPx != -1f) {
                fontSizeInPx = pixelsToSp(fontSizeInPx)
                lynxConfig!!.textSizeInPx = fontSizeInPx
            }
            val samplingRate = attributes.getInteger(
                R.styleable.lynx_sampling_rate, lynxConfig!!.samplingRate
            )
            lynxConfig!!
                .setMaxNumberOfTracesToShow(maxTracesToShow)
                .setFilter(if (TextUtils.isEmpty(filter)) "" else filter).samplingRate =
                samplingRate
            attributes.recycle()
        }
    }

    private fun initializeView() {
        val context = context
        val layoutInflater = LayoutInflater.from(context)
        layoutInflater.inflate(R.layout.lynx_view, this)
        mapGui()
        initializeRenderers()
        hookListeners()
    }

    private fun mapGui() {
        lvTraces = findViewById<View>(R.id.lv_traces) as ListView
        lvTraces!!.transcriptMode = AbsListView.TRANSCRIPT_MODE_ALWAYS_SCROLL
        etFilter = findViewById<View>(R.id.et_filter) as EditText
        spFilter = findViewById<View>(R.id.sp_filter) as Spinner
        configureCursorColor()
        updateFilterText()
    }

    /**
     * Changes EditText cursor color either with an official API (on API level >= 29) or through
     * reflection on older levels.
     */
    @SuppressLint("DiscouragedPrivateApi")
    private fun configureCursorColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            etFilter!!.setTextCursorDrawable(R.drawable.edit_text_cursor_color)
        } else {
            try {
                val f = TextView::class.java.getDeclaredField("mCursorDrawableRes")
                f.isAccessible = true
                f[etFilter] = R.drawable.edit_text_cursor_color
            } catch (e: Exception) {
                Log.e(TAG, "Error trying to change cursor color text cursor drawable to null.")
            }
        }
    }

    private fun initializeRenderers() {
        val tracesRendererBuilder: RendererBuilder<Trace> = TraceRendererBuilder(lynxConfig)
        adapter = RendererAdapter(tracesRendererBuilder)
        adapter!!.addAll(presenter!!.currentTraces)
        if (adapter!!.count > 0) {
            adapter!!.notifyDataSetChanged()
        }
        lvTraces!!.adapter = adapter
    }

    private fun hookListeners() {
        lvTraces!!.setOnScrollListener(
            object : AbsListView.OnScrollListener {
                override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                    // Empty
                }

                override fun onScroll(
                    view: AbsListView,
                    firstVisibleItem: Int,
                    visibleItemCount: Int,
                    totalItemCount: Int
                ) {
                    // Hack to avoid problems with the scroll position when auto scroll is
                    // disabled. This hack
                    // is needed because Android notify a firstVisibleItem one position before
                    // it should be.
                    if (lastScrollPosition - firstVisibleItem != 1) {
                        lastScrollPosition = firstVisibleItem
                    }
                    val lastVisiblePositionInTheList = firstVisibleItem + visibleItemCount
                    presenter!!.onScrollToPosition(lastVisiblePositionInTheList)
                }
            })
        etFilter!!.addTextChangedListener(
            object : TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                    // Empty
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                    presenter!!.updateFilter(s.toString())
                }

                override fun afterTextChanged(s: Editable) {
                    // Empty
                }
            })
        val adapter = ArrayAdapter(
            context, R.layout.single_line_spinner_item, TraceLevel.values()
        )
        spFilter!!.adapter = adapter
        spFilter!!.setSelection(DEFAULT_POSITION)
        spFilter!!.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View, position: Int, id: Long
            ) {
                presenter!!.updateFilterTraceLevel(
                    parent.getItemAtPosition(position) as TraceLevel
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun initializePresenter() {
        val lynx = Lynx(Logcat(), AndroidMainThread(), TimeProvider())
        lynx.config = lynxConfig!!
        presenter = LynxPresenter(lynx, this, lynxConfig!!.maxNumberOfTracesToShow)
    }

    private fun validateLynxConfig(lynxConfig: LynxConfig?) {
        requireNotNull(lynxConfig) { "You can't configure Lynx with a null LynxConfig instance." }
    }

    private fun updateFilterText() {
        if (lynxConfig!!.hasFilter()) {
            etFilter!!.append(lynxConfig!!.filter)
        }
    }

    private fun updateAdapter() {
        if (lynxConfig!!.hasTextSizeInPx()
            && lynxConfig!!.textSizeInPx != lynxConfig!!.textSizeInPx
        ) {
            initializeRenderers()
        }
    }

    private fun pixelsToSp(px: Float): Float {
        val scaledDensity = context.resources.displayMetrics.scaledDensity
        return px / scaledDensity
    }

    private fun updateScrollPosition(removedTraces: Int) {
        val shouldUpdateScrollPosition = removedTraces > 0
        if (shouldUpdateScrollPosition) {
            val newScrollPosition = lastScrollPosition - removedTraces
            lastScrollPosition = newScrollPosition
            lvTraces!!.setSelectionFromTop(newScrollPosition, 0)
        }
    }

    companion object {
        private const val TAG = "LynxView"
        private const val DEFAULT_POSITION = 0
    }
}