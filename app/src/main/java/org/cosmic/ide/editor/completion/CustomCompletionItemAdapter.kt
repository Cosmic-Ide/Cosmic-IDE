package org.cosmic.ide.editor.completion

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.color.MaterialColors
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import org.cosmic.ide.R

class CustomCompletionItemAdapter : EditorCompletionAdapter() {

    override fun getItemHeight(): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 48f, getContext().getResources().getDisplayMetrics()).toInt()
    }

    override fun getView(pos: Int, v: View?, parent: ViewGroup, isCurrentCursorPosition: Boolean): View {
        val view = LayoutInflater.from(getContext()).inflate(R.layout.custom_completion_result_item, parent, false)
        if (isCurrentCursorPosition) {
            val color = MaterialColors.getColor(getContext(), com.google.android.material.R.attr.colorControlHighlight, Color.TRANSPARENT)
            view.setBackgroundColor(color)
        } else {
            view.setBackground(null)
        }
        val item = getItem(pos)
        var tv: TextView = view.findViewById(R.id.result_item_label)
        tv.setText(item.label)
        tv = view.findViewById(R.id.result_item_desc)
        tv.setText(item.desc)
        view.setTag(pos)
        val iv: TextView = view.findViewById(R.id.result_item_image)
        iv.setText(item.desc.subSequence(0, 1))
        return view
    }
}
