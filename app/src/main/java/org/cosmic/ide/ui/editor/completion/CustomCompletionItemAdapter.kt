package org.cosmic.ide.ui.editor.completion

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import org.cosmic.ide.R
import org.cosmic.ide.util.AndroidUtilities

class CustomCompletionItemAdapter : EditorCompletionAdapter() {

    override fun getItemHeight(): Int {
        return AndroidUtilities.dp(45f)
    }

    override fun getView(pos: Int, v: View?, parent: ViewGroup, isCurrentCursorPosition: Boolean): View {
        val view = LayoutInflater.from(context).inflate(R.layout.custom_completion_result_item, parent, false)
        val item = getItem(pos)
        var tv: TextView = view.findViewById(R.id.result_item_label)
        tv.text = item.label
        tv = view.findViewById(R.id.result_item_desc)
        tv.text = item.desc
        if (item.desc.length > 0) {
            tv = view.findViewById(R.id.result_item_image)
            tv.text = item.desc.subSequence(0, 1)
        }
        return view
    }
}
