package com.pranav.java.ide.editor.completion;

import android.graphics.Color;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.pranav.java.ide.R;

import com.google.android.material.color.MaterialColors;

import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter;
import io.github.rosemoe.sora.lang.completion.CompletionItem;

public class CustomCompletionItemAdapter extends EditorCompletionAdapter {

    @Override
    public int getItemHeight() {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48, getContext().getResources().getDisplayMetrics());
    }

    @Override
    public View getView(int pos, View view, ViewGroup parent, boolean isCurrentCursorPosition) {
        if (view == null) {
            view = LayoutInflater.from(getContext()).inflate(R.layout.custom_completion_result_item, parent, false);
        }
        if(isCurrentCursorPosition) {
            int color = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorControlHighlight, Color.TRANSPARENT);
            view.setBackgroundColor(color);
        } else {
            view.setBackground(null);
        }
        var item = getItem(pos);
        TextView tv = view.findViewById(R.id.result_item_label);
        tv.setText(item.label);
        tv = view.findViewById(R.id.result_item_desc);
        tv.setText(item.desc);
        view.setTag(pos);
        TextView iv = view.findViewById(R.id.result_item_image);
        iv.setText(item.desc.subSequence(0, 1));
        return view;
    }

}