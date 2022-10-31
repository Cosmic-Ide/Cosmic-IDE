package org.cosmic.ide.ui.editor.completion

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import io.github.rosemoe.sora.widget.schemes.EditorColorScheme
import org.cosmic.ide.R

class CustomCompletionItemAdapter : EditorCompletionAdapter() {

    override fun getItemHeight(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, getContext().getResources().getDisplayMetrics()).toInt()
    }

    override fun getView(pos: Int, v: View?, parent: ViewGroup, isCurrentCursorPosition: Boolean): View {
        val view = LayoutInflater.from(getContext()).inflate(R.layout.custom_completion_result_item, parent, false)
        val item = getItem(pos)
        var tv: TextView = view.findViewById(R.id.result_item_label)
        tv.setText(item.label)
        tv.setTextColor(MaterialColors.getColor(getContext(), android.R.attr.textColorPrimary, getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY)))
        tv = view.findViewById(R.id.result_item_desc)
        tv.setText(item.desc)
        tv.setTextColor(MaterialColors.getColor(getContext(), android.R.attr.textColorSecondary, getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_SECONDARY)))
        view.setTag(pos)
        tv = view.findViewById(R.id.result_item_image)
        tv.setText(item.desc.subSequence(0, 1))
        tv.setTextColor(MaterialColors.getColor(getContext(), android.R.attr.textColorPrimary, getThemeColor(EditorColorScheme.COMPLETION_WND_TEXT_PRIMARY)))
        return view
    }
}
