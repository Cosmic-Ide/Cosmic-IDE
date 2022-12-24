package org.cosmic.ide.ui.editor

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.elevation.SurfaceColors
import io.github.rosemoe.sora.widget.CodeEditor
import org.cosmic.ide.util.resolveAttr

class SymbolInputView : LinearLayout {
    private var textColor = 0
    private lateinit var editor: CodeEditor

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    init {
        setBackgroundColor(SurfaceColors.SURFACE_1.getColor(context))
        setTextColor(context.resolveAttr(com.google.android.material.R.attr.colorOnSurface))
        orientation = HORIZONTAL

        ViewCompat.setOnApplyWindowInsetsListener(
            this
        ) { _: View?, insets: WindowInsetsCompat ->
            val bottomInset: Int = if (insets.isVisible(WindowInsetsCompat.Type.ime())) {
                insets.getInsets(WindowInsetsCompat.Type.ime()).bottom
            } else {
                insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            }
            setPadding(0, 0, 0, bottomInset)
            insets
        }
    }

    fun bindEditor(editor: CodeEditor): SymbolInputView {
        this.editor = editor
        return this
    }

    fun setTextColor(color: Int): SymbolInputView {
        for (i in 0 until childCount) {
            (getChildAt(i) as Button).setTextColor(color)
        }
        textColor = color
        return this
    }

    fun addSymbols(symbols: Array<String>): SymbolInputView {
        for (symbol in symbols) addSymbol(symbol)
        return this
    }

    fun addSymbol(
        display: String,
        content: String = display,
        cursorPos: Int = content.length
    ): SymbolInputView {
        val btn = Button(context, null, android.R.attr.buttonStyleSmall)
        btn.text = display
        val out = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, out, true)
        btn.setBackgroundResource(out.resourceId)
        btn.setTextColor(textColor)
        addView(btn, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT))

        btn.setOnClickListener {
            if (this::editor.isInitialized) editor.insertText(content, cursorPos)
        }
        return this
    }
}
