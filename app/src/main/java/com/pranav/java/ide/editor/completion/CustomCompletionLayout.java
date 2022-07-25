package com.pranav.java.ide.editor.completion;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import io.github.rosemoe.sora.widget.component.CompletionLayout;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme;

public class CustomCompletionLayout implements CompletionLayout {

    private ListView mListView;
    private ProgressBar mProgressBar;
    private GradientDrawable mBackground;
    private EditorAutoCompletion mEditorAutoCompletion;

    @Override
    public void setEditorCompletion(EditorAutoCompletion completion) {
        mEditorAutoCompletion = completion;
    }

    @Override
    public View inflate(Context context) {
        RelativeLayout layout = new RelativeLayout(context);

        mProgressBar = new ProgressBar(context);
        layout.addView(mProgressBar);
        var params = ((RelativeLayout.LayoutParams) mProgressBar.getLayoutParams());
        params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        params.width = params.height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, context.getResources().getDisplayMetrics());

        mBackground = new GradientDrawable(); 
        mBackground.setCornerRadius(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, context.getResources().getDisplayMetrics()));
        layout.setBackground(mBackground);

        mListView = new ListView(context);
        mListView.setDividerHeight(0);
        layout.addView(mListView, new LinearLayout.LayoutParams(-1, -1)); 
        mListView.setOnItemClickListener((parent, view, position, id) -> {
            try {
                mEditorAutoCompletion.select(position);
            } catch (Exception e) {
                Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        setLoading(true);
        return layout;
    }

    @Override
    public void onApplyColorScheme(EditorColorScheme colorScheme) {
        mBackground.setStroke(1, colorScheme.getColor(EditorColorScheme.COMPLETION_WND_CORNER));
        mBackground.setColor(colorScheme.getColor(EditorColorScheme.COMPLETION_WND_BACKGROUND));
    }

    @Override
    public void setLoading(boolean state) {
        mProgressBar.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    public ListView getCompletionList() {
        return mListView;
    }

    /**
     * Perform motion events
     */
    private void performScrollList(int offset) {
        var adpView = getCompletionList();

        long down = SystemClock.uptimeMillis();
        var ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_DOWN, 0, 0, 0);
        adpView.onTouchEvent(ev);
        ev.recycle();

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_MOVE, 0, offset, 0);
        adpView.onTouchEvent(ev);
        ev.recycle();

        ev = MotionEvent.obtain(down, down, MotionEvent.ACTION_CANCEL, 0, offset, 0);
        adpView.onTouchEvent(ev);
        ev.recycle();
    }

    @Override
    public void ensureListPositionVisible(int position, int increment) {
        mListView.post(() -> {
            while (mListView.getFirstVisiblePosition() + 1 > position && mListView.canScrollList(-1)) {
                performScrollList(increment / 2);
            }
            while (mListView.getLastVisiblePosition() - 1 < position && mListView.canScrollList(1)) {
                performScrollList(-increment / 2);
            }
        });
    }
}
