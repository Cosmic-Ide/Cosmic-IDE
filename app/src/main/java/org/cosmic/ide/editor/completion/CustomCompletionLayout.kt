package org.cosmic.ide.editor.completion

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
import android.widget.Toast

import io.github.rosemoe.sora.widget.component.CompletionLayout
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme

class CustomCompletionLayout: CompletionLayout {

    private lateinit var mListView: ListView
    private lateinit var mProgressBar: ProgressBar
    private lateinit var mBackground: GradientDrawable
    private lateinit var mEditorAutoCompletion: EditorAutoCompletion

    override fun setEditorCompletion(completion: EditorAutoCompletion) {
        mEditorAutoCompletion = completion
    }

    override fun inflate(context: Context): View {
        val layout = RelativeLayout(context)

        mProgressBar = ProgressBar(context)
        layout.addView(mProgressBar)
        val params =
            mProgressBar.getLayoutParams() as RelativeLayout.LayoutParams
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT)
        params.width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30f, context.getResources().getDisplayMetrics()).toInt()
        params.height = params.width
        mBackground = GradientDrawable()
        mBackground.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, context.getResources().getDisplayMetrics()))
        layout.setBackground(mBackground)

        mListView = ListView(context)
        mListView.setDividerHeight(0)
        layout.addView(mListView, LinearLayout.LayoutParams(-1, -1));
        mListView.setOnItemClickListener {
            _, _, position, _ ->
                try {
                    mEditorAutoCompletion.select(position)
                } catch (e: Exception) {
                    Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show()
                }
        };
        setLoading(true)
        return layout
    }

    override fun onApplyColorScheme(colorScheme: EditorColorScheme) {
        mBackground.setStroke(1, colorScheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER))
        mBackground.setColor(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND))
    }

    override fun setLoading(state: Boolean) {
        val visibility = if (state) View.VISIBLE else View.INVISIBLE
        mProgressBar.setVisibility(visibility)
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
