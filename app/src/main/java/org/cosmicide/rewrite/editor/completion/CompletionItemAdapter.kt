package org.cosmicide.rewrite.editor.completion

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import org.cosmicide.rewrite.databinding.CompletionResultBinding
import org.cosmicide.rewrite.editor.EditorCompletionItem


class CustomCompletionItemAdapter : EditorCompletionAdapter() {

    override fun areAllItemsEnabled(): Boolean {
        return false
    }

    override fun getItemHeight(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            65f,
            context.resources.displayMetrics
        ).toInt()
    }

    override fun getItem(position: Int): EditorCompletionItem {
        return super.getItem(position) as EditorCompletionItem
    }

    override fun getView(
        pos: Int,
        v: View?,
        parent: ViewGroup,
        isCurrentCursorPosition: Boolean
    ): View {
        val binding = CompletionResultBinding.inflate(LayoutInflater.from(context))
        val item = getItem(pos)
        binding.resultItemLabel.text = item.label
        binding.resultItemDesc.text = item.desc
        binding.resultItemIcon.setImageDrawable(item.icon)
        return binding.root
    }
}