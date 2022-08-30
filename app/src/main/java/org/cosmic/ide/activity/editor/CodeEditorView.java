package org.cosmic.ide.activity.editor;

import android.content.Context;
import android.util.AttributeSet;

import org.cosmic.ide.activity.editor.completion.CustomCompletionItemAdapter;
import org.cosmic.ide.activity.editor.completion.CustomCompletionLayout;

import java.io.File;

import io.github.rosemoe.sora.widget.CodeEditor;
import io.github.rosemoe.sora.widget.component.EditorAutoCompletion;

public class CodeEditorView extends CodeEditor {

    private File mCurrentFile;

    public CodeEditorView(Context context) {
        this(context, null);
    }

    public CodeEditorView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CodeEditorView(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CodeEditorView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init();
    }

    private void init() {
        getComponent(EditorAutoCompletion.class).setLayout(new CustomCompletionLayout());
        getComponent(EditorAutoCompletion.class).setAdapter(new CustomCompletionItemAdapter());
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        hideEditorWindows();
    }
}