package org.cosmicide.editor.completion

import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.InsetDrawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.drawable.toDrawable
import io.github.rosemoe.sora.lang.completion.CompletionItem
import io.github.rosemoe.sora.lang.completion.CompletionItemKind
import io.github.rosemoe.sora.widget.component.EditorCompletionAdapter
import org.cosmicide.databinding.CompletionResultBinding

class CustomCompletionItemAdapter(
    private val colors: ColorScheme
) : EditorCompletionAdapter() {

    override fun areAllItemsEnabled(): Boolean = true

    override fun getItemHeight(): Int {
        return dp(56f).toInt()
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
        val kindColor = kindColor(item.kind)

        binding.resultItemLabel.text = item.label
        binding.resultItemLabel.setTextColor(colors.onSurface.toArgb())
        binding.resultItemDesc.apply {
            visibility = if (item.desc.isNullOrBlank()) View.GONE else View.VISIBLE
            text = item.desc?.toString()
            setTextColor(colors.onSurfaceVariant.toArgb())
        }

        if (item.deprecated) {
            binding.resultItemLabel.paintFlags =
                binding.resultItemLabel.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.resultItemLabel.alpha = 0.58f
        } else {
            binding.resultItemLabel.paintFlags =
                binding.resultItemLabel.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            binding.resultItemLabel.alpha = 1f
        }

        binding.resultItemIcon.apply {
            text = item.kind?.name?.firstOrNull()?.uppercaseChar()?.toString() ?: "T"
            setTextColor(kindColor)
            background = roundedBackground(
                color = ColorUtils.setAlphaComponent(kindColor, 32),
                radiusDp = 7f
            )
            contentDescription = item.kind?.name ?: context.getString(
                org.cosmicide.R.string.completion_type
            )

            if (item.deprecated) paintFlags += Paint.STRIKE_THRU_TEXT_FLAG
        }

        binding.resultItemDetail.apply {
            if (item.detail.isNullOrBlank()) {
                visibility = View.GONE
                text = null
            } else {
                visibility = View.VISIBLE
                text = item.detail
                setTextColor(colors.onSurfaceVariant.toArgb())
                background = roundedBackground(
                    color = colors.surfaceContainerHighest.toArgb(),
                    radiusDp = 6f
                )
            }
        }

        binding.root.minimumHeight = dp(56f).toInt()
        binding.root.background = if (isCurrentCursorPosition) {
            InsetDrawable(
                roundedBackground(
                    color = colors.secondaryContainer.copy(alpha = 0.72f).toArgb(),
                    radiusDp = 9f,
                    strokeColor = colors.secondary.copy(alpha = 0.28f).toArgb()
                ),
                dp(4f).toInt(),
                dp(2f).toInt(),
                dp(4f).toInt(),
                dp(2f).toInt()
            )
        } else {
            Color.TRANSPARENT.toDrawable()
        }

        return binding.root
    }

    private fun kindColor(kind: CompletionItemKind?): Int = when (kind) {
        CompletionItemKind.Method,
        CompletionItemKind.Function,
        CompletionItemKind.Constructor,
        CompletionItemKind.Operator -> colors.tertiaryFixedDim

        CompletionItemKind.Class,
        CompletionItemKind.Interface,
        CompletionItemKind.Struct,
        CompletionItemKind.Enum,
        CompletionItemKind.TypeParameter -> colors.primary

        CompletionItemKind.Field,
        CompletionItemKind.Property,
        CompletionItemKind.Variable,
        CompletionItemKind.EnumMember -> colors.secondary

        CompletionItemKind.Module,
        CompletionItemKind.Folder,
        CompletionItemKind.File -> colors.tertiary

        CompletionItemKind.Keyword,
        CompletionItemKind.Snippet,
        CompletionItemKind.Text -> colors.primary

        CompletionItemKind.Constant,
        CompletionItemKind.Value,
        CompletionItemKind.Color -> colors.error

        else -> colors.onSurfaceVariant
    }.toArgb()

    private fun roundedBackground(
        color: Int,
        radiusDp: Float,
        strokeColor: Int? = null
    ): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = dp(radiusDp)
            setColor(color)
            strokeColor?.let { setStroke(dp(1f).toInt(), it) }
        }
    }

    private fun dp(value: Float): Float {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value,
            context.resources.displayMetrics
        )
    }
}
