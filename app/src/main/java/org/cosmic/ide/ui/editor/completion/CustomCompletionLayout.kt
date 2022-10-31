package org.cosmic.ide.ui.editor.completion

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.SystemClock
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import com.google.android.material.color.MaterialColors
import io.github.rosemoe.sora.widget.component.CompletionLayout
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class CustomCompletionLayout : CompletionLayout {

    private lateinit var mListView: ListView
    private lateinit var mBackground: GradientDrawable
    private lateinit var mEditorAutoCompletion: EditorAutoCompletion

    override fun setEditorCompletion(completion: EditorAutoCompletion) {
        mEditorAutoCompletion = completion
    }

    override fun inflate(context: Context): View {
        val layout = RelativeLayout(context)

        mBackground = GradientDrawable()
        mBackground.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.getResources().getDisplayMetrics()))
        layout.setBackground(mBackground)

        mListView = ListView(context)
        mListView.setDividerHeight(0)
        layout.addView(mListView, LinearLayout.LayoutParams(-1, -1))
        mListView.setOnItemClickListener {
            _, _, position, _ ->
            mEditorAutoCompletion.select(position)
        }
        return layout
    }

    override fun onApplyColorScheme(colorScheme: EditorColorScheme) {
        mBackground.setColor(MaterialColors.getColor(mEditorAutoCompletion.getContext(), com.google.android.material.R.attr.colorSurface, colorScheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND)))
    }

    override fun setLoading(state: Boolean) {
        
    }

    override fun getCompletionList(): ListView {
        return mListView
    }

    private fun performScrollList(offset: Int) {
        val adpView = getCompletionList()

        val down = SystemClock.uptimeMillis()
        var ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 0f, 0f, 0)
        adpView.onTouchEvent(ev)
        ev.recycle()

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_MOVE, 0f, offset.toFloat(), 0)
        adpView.onTouchEvent(ev)
        ev.recycle()

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_CANCEL, 0f, offset.toFloat(), 0)
        adpView.onTouchEvent(ev)
        ev.recycle()
    }

    override fun ensureListPositionVisible(position: Int, increment: Int) {
        mListView.post {
            while (mListView.getFirstVisiblePosition() + 1 > position && mListView.canScrollList(-1)) {
                performScrollList(increment / 2)
            }
            while (mListView.getLastVisiblePosition() - 1 < position && mListView.canScrollList(1)) {
                performScrollList(-increment / 2)
            }
        }
    }
}
