package org.cosmic.ide.ui.editor

import android.content.Context
import android.util.AttributeSet
import android.util.TypedValue
import android.widget.Button
import android.widget.LinearLayout
import com.google.android.material.color.MaterialColors
import io.github.rosemoe.sora.widget.CodeEditor

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
        setBackgroundColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurface))
        setTextColor(MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurface))
        orientation = HORIZONTAL
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