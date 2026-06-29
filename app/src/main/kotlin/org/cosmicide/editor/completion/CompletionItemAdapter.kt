package org.cosmicide.editor.completion

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.color.MaterialColors
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.lsp.editor.completion.LspCompletionItem
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import org.cosmicide.databinding.CompletionResultBinding

class CustomCompletionItemAdapter : EditorCompletionAdapter() {

    override fun areAllItemsEnabled(): Boolean = true

    override fun getItemHeight(): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            52f,
            context.resources.displayMetrics
        ).toInt()
    }

    override fun getView(
        pos: Int,
        v: View?,
        parent: ViewGroup,
        isCurrentCursorPosition: Boolean
    ): View {
        val binding = v?.let { CompletionResultBinding.bind(it) }
            ?: CompletionResultBinding.inflate(LayoutInflater.from(context), parent, false)

        val item: CompletionItem = super.getItem(pos)

        // 1. Bind Label and Description
        binding.resultItemLabel.text = item.label
        binding.resultItemDesc.text = item.desc

        // 2. Handle Deprecation (The Strikethrough Effect)
        if (item.deprecated) {
            binding.resultItemLabel.paintFlags = binding.resultItemLabel.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.resultItemLabel.alpha = 0.6f // Fade it out slightly
        } else {
            binding.resultItemLabel.paintFlags = binding.resultItemLabel.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.resultItemLabel.alpha = 1.0f
        }

        // 3. Semantic Kind Coloring (The "IDE" Magic)
        val kindColor = getKindColor(item.kind)

        // Apply a subtle rounded background to the icon (15% opacity)
        val iconBackground = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, context.resources.displayMetrics)
            setColor(Color.argb(38, Color.red(kindColor), Color.green(kindColor), Color.blue(kindColor)))
        }
        binding.resultItemIcon.background = iconBackground
        binding.resultItemIcon.setImageDrawable(item.icon)

        // 4. Smart Detail Formatting & Coloring
        val rawDetail = item.detail
        val formattedDetail = formatDetail(rawDetail)

        if (!formattedDetail.isNullOrEmpty()) {
            binding.resultItemDetail.text = formattedDetail
            binding.resultItemDetail.setTextColor(kindColor) // Color the detail text!
            binding.resultItemDetail.visibility = View.VISIBLE
        } else {
            binding.resultItemDetail.visibility = View.GONE
        }

        // 5. Extract Literal Colors (For things like Color.parseColor("#FF0000"))
        val extractedColor = (item as? LspCompletionItem)?.extractColor()
        if (extractedColor != null && item.kind == CompletionItemKind.Color) {
            // If it's a literal color, override the icon background with the actual color
            val colorBackground = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 6f, context.resources.displayMetrics)
                setColor(extractedColor)
            }
            binding.resultItemIcon.background = colorBackground
        }

        // 6. Material 3 Selection Highlighting
        if (isCurrentCursorPosition) {
            binding.root.setBackgroundColor(
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurfaceVariant, null)
            )
        } else {
            binding.root.setBackgroundColor(
                MaterialColors.getColor(context, com.google.android.material.R.attr.colorSurface, null)
            )
        }

        return binding.root
    }

    // --- Helper: Maps LSP Kinds to Beautiful IDE Colors ---
    private fun getKindColor(kind: CompletionItemKind?): Int {
        return when (kind) {
            CompletionItemKind.Method, CompletionItemKind.Function -> Color.parseColor("#C792EA") // Purple (Methods)
            CompletionItemKind.Class, CompletionItemKind.Interface -> Color.parseColor("#FFCB6B") // Yellow (Classes)
            CompletionItemKind.Field, CompletionItemKind.Property -> Color.parseColor("#82AAFF") // Blue (Fields)
            CompletionItemKind.Variable -> Color.parseColor("#4DB6AC") // Teal (Variables)
            CompletionItemKind.EnumMember -> Color.parseColor("#4DB6AC")
            CompletionItemKind.Module -> Color.parseColor("#F78C6C") // Orange (Packages)
            CompletionItemKind.Keyword -> Color.parseColor("#89DDFF") // Cyan (Keywords)
            CompletionItemKind.Snippet -> Color.parseColor("#C3E88D") // Green (Snippets)
            CompletionItemKind.Constant -> Color.parseColor("#F78C6C") // Orange (Constants)
            else -> Color.parseColor("#90A4AE") // Grey (Default)
        }
    }

    private fun formatDetail(detail: CharSequence?): String? {
        if (detail.isNullOrEmpty()) return null
        return detail.toString()
            .replace("java.lang.", "")
            .replace("java.util.", "")
            .replace("java.io.", "")
            .replace("android.", "")
            .replace("androidx.", "")
            .replace("org.json.", "")
    }
}