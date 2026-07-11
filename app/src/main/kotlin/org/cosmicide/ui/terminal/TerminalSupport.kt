package org.cosmicide.ui.terminal

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.termux.terminal.TerminalColors
import com.termux.terminal.TerminalSession
import com.termux.terminal.TextStyle
import com.termux.view.TerminalRenderer
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient

internal const val MinTerminalTextSizeDp = 4
internal const val MaxTerminalTextSizeDp = 40
internal const val TerminalZoomOutThreshold = 0.9f
internal const val TerminalZoomInThreshold = 1.1f
internal const val TerminalTranscriptRows = 2000

internal const val Escape = "\u001B"
internal const val EndOfText = 3

internal class TerminalModifierLatch {
    var ctrl by mutableStateOf(false)
    var alt by mutableStateOf(false)
}

internal data class TerminalGeometry(
    val columns: Int,
    val rows: Int,
    val cellWidthPixels: Int,
    val cellHeightPixels: Int
)

internal data class ExtraKey(
    val label: String,
    val keyCode: Int? = null,
    val codePoint: Int? = null,
    val output: String? = null
)

internal val TerminalExtraKeys = listOf(
    ExtraKey("TAB", KeyEvent.KEYCODE_TAB),
    ExtraKey("/", codePoint = '/'.code),
    ExtraKey("-", codePoint = '-'.code),
    ExtraKey("|", codePoint = '|'.code),
    ExtraKey("HOME", output = "\u0001"),
    ExtraKey("END", output = "\u0005"),
    ExtraKey("PGUP", KeyEvent.KEYCODE_PAGE_UP),
    ExtraKey("PGDN", KeyEvent.KEYCODE_PAGE_DOWN),
    ExtraKey("INS", KeyEvent.KEYCODE_INSERT),
    ExtraKey("DEL", KeyEvent.KEYCODE_FORWARD_DEL),
    ExtraKey("BKSP", KeyEvent.KEYCODE_DEL),
    ExtraKey("UP", KeyEvent.KEYCODE_DPAD_UP),
    ExtraKey("DOWN", KeyEvent.KEYCODE_DPAD_DOWN),
    ExtraKey("LEFT", KeyEvent.KEYCODE_DPAD_LEFT),
    ExtraKey("RIGHT", KeyEvent.KEYCODE_DPAD_RIGHT),
    ExtraKey("ENTER", KeyEvent.KEYCODE_ENTER),
    ExtraKey("F1", KeyEvent.KEYCODE_F1),
    ExtraKey("F2", KeyEvent.KEYCODE_F2),
    ExtraKey("F3", KeyEvent.KEYCODE_F3),
    ExtraKey("F4", KeyEvent.KEYCODE_F4)
)

internal class TerminalInputBridge(
    private val context: Context
) {
    private var controller: TerminalController? = null
    private var clearingText = false

    val editText: EditText = EditText(context).apply {
        layoutParams = FrameLayout.LayoutParams(1, 1)
        setBackgroundColor(Color.Transparent.toArgb())
        setTextColor(Color.Transparent.toArgb())
        setSingleLine(false)
        isCursorVisible = false
        includeFontPadding = false

        inputType = InputType.TYPE_CLASS_TEXT or
                InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS or
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                InputType.TYPE_TEXT_FLAG_MULTI_LINE

        imeOptions = EditorInfo.IME_ACTION_NONE or
                EditorInfo.IME_FLAG_NO_EXTRACT_UI or
                EditorInfo.IME_FLAG_NO_FULLSCREEN

        setOnKeyListener { _, keyCode, event ->
            controller?.handleKeyEvent(keyCode, event) == true
        }

        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) = Unit

            override fun afterTextChanged(s: Editable) {
                if (clearingText || s.isEmpty()) return

                controller?.writeInputText(s.toString())

                clearingText = true
                s.clear()
                clearingText = false
            }
        })
    }

    fun attachController(controller: TerminalController) {
        this.controller = controller
    }

    fun showKeyboard() {
        editText.requestFocus()

        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(editText, 0)
    }
}

internal class BasicTerminalViewClient(
    private val controller: TerminalController,
    private val showKeyboard: () -> Unit,
    private val onZoom: (increase: Boolean) -> Unit
) : TerminalViewClient {
    override fun onScale(scale: Float): Float {
        return when {
            scale < TerminalZoomOutThreshold -> {
                onZoom(false)
                1f
            }

            scale > TerminalZoomInThreshold -> {
                onZoom(true)
                1f
            }

            else -> scale
        }
    }

    override fun onSingleTapUp(e: MotionEvent) {
        showKeyboard()
    }

    override fun onKeyDown(
        keyCode: Int,
        e: KeyEvent,
        session: TerminalSession?
    ): Boolean {
        controller.handleKeyEvent(keyCode, e)
        return true
    }

    override fun onCodePoint(
        codePoint: Int,
        ctrlDown: Boolean,
        session: TerminalSession?
    ): Boolean {
        controller.writeTerminalCodePoint(
            prependEscape = false,
            codePoint = codePoint.toTerminalCodePoint(ctrlDown)
        )
        return true
    }

    override fun shouldBackButtonBeMappedToEscape(): Boolean = false
    override fun shouldEnforceCharBasedInput(): Boolean = false
    override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
    override fun isTerminalViewSelected(): Boolean = true
    override fun copyModeChanged(copyMode: Boolean) = Unit
    override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
    override fun onLongPress(event: MotionEvent): Boolean = false
    override fun readControlKey(): Boolean = false
    override fun readAltKey(): Boolean = false
    override fun readShiftKey(): Boolean = false
    override fun readFnKey(): Boolean = false
    override fun onEmulatorSet() = Unit

    override fun logError(tag: String, message: String) = Unit
    override fun logWarn(tag: String, message: String) = Unit
    override fun logInfo(tag: String, message: String) = Unit
    override fun logDebug(tag: String, message: String) = Unit
    override fun logVerbose(tag: String, message: String) = Unit
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) = Unit
    override fun logStackTrace(tag: String, e: Exception) = Unit
}

internal fun TerminalView.applyTerminalAppearance(
    textSizeDp: Int,
    typeface: Typeface
) {
    val textSizePx = context.dpToPx(textSizeDp.toFloat())
    mRenderer = TerminalRenderer(textSizePx.toInt(), typeface)
    invalidate()
}

internal fun TerminalView.calculateGeometry(
    textSizeDp: Int,
    typeface: Typeface
): TerminalGeometry? {
    val viewWidth = width
    val viewHeight = height
    if (viewWidth <= 0 || viewHeight <= 0) return null

    val textSizePx = context.dpToPx(textSizeDp.toFloat())

    val paint = Paint().apply {
        isAntiAlias = true
        this.typeface = typeface
        textSize = textSizePx
    }

    val cellWidth = paint.measureText("X").coerceAtLeast(1f)
    val metrics = paint.fontMetricsInt
    val cellHeight = (metrics.descent - metrics.ascent)
        .coerceAtLeast(1)

    return TerminalGeometry(
        columns = maxOf(4, (viewWidth / cellWidth).toInt()),
        rows = maxOf(4, viewHeight / cellHeight),
        cellWidthPixels = cellWidth.toInt().coerceAtLeast(1),
        cellHeightPixels = cellHeight + 4
    )
}

internal fun applyTerminalColors(colorScheme: ColorScheme) {
    val colors = TerminalColors.COLOR_SCHEME.mDefaultColors

    val fg = colorScheme.onSurfaceVariant.toArgb()
    val bg = colorScheme.surfaceContainer.toArgb()

    colors[0] = colorScheme.onSurface.toArgb()
    colors[1] = dim(Color.Red)
    colors[2] = colorScheme.primary.toArgb()
    colors[3] = dim(Color.Yellow)
    colors[4] = dim(Color.Blue, 0.65f)
    colors[5] = dim(Color.Magenta)
    colors[6] = dim(Color.Cyan)
    colors[7] = colorScheme.onSurfaceVariant.toArgb()

    colors[8] = Color.DarkGray.toArgb()
    colors[9] = Color.Red.toArgb()
    colors[10] = colorScheme.primary.toArgb()
    colors[11] = Color.Yellow.toArgb()
    colors[12] = Color.Blue.toArgb()
    colors[13] = Color.Magenta.toArgb()
    colors[14] = Color.Cyan.toArgb()
    colors[15] = fg

    colors[TextStyle.COLOR_INDEX_FOREGROUND] = fg
    colors[TextStyle.COLOR_INDEX_BACKGROUND] = bg
    colors[TextStyle.COLOR_INDEX_CURSOR] = colorScheme.secondary.toArgb()
}

internal fun Int.toTerminalCodePoint(ctrlPressed: Boolean): Int {
    if (!ctrlPressed) return this

    return when (this) {
        in 'a'.code..'z'.code -> this - 'a'.code + 1
        in 'A'.code..'Z'.code -> this - 'A'.code + 1

        '['.code, '3'.code -> 27
        '\\'.code, '4'.code -> 28
        ']'.code, '5'.code -> 29
        '^'.code, '6'.code -> 30
        '_'.code, '7'.code, '/'.code -> 31
        '8'.code, '?'.code -> 127

        else -> this
    }
}

internal fun Char.controlChar(): String {
    val upper = uppercaseChar()

    return if (upper in 'A'..'Z') {
        ((upper.code - 'A'.code + 1).toChar()).toString()
    } else {
        toString()
    }
}

private fun dim(color: Color, factor: Float = 0.80f): Int {
    val r = (color.red * 255f * factor).toInt().coerceIn(0, 255)
    val g = (color.green * 255f * factor).toInt().coerceIn(0, 255)
    val b = (color.blue * 255f * factor).toInt().coerceIn(0, 255)

    return (0xff shl 24) or
            (r shl 16) or
            (g shl 8) or
            b
}

private fun Context.dpToPx(dp: Float): Float {
    return dp * resources.displayMetrics.density
}