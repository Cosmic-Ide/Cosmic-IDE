package org.cosmic.ide.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.DialogInterface
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import org.cosmic.ide.App
import org.cosmic.ide.R
import kotlin.math.roundToInt

object AndroidUtilities {
    fun showToast(message: String?) {
        Toast.makeText(App.context, message, Toast.LENGTH_SHORT).show()
    }

    fun dp(px: Float): Int {
        return (App.context.resources.displayMetrics.density * px).roundToInt()
    }

    fun setMargins(
        view: View, startMargin: Int, topMargin: Int, endMargin: Int, bottomMargin: Int
    ) {
        val layoutParams = view.layoutParams as MarginLayoutParams
        layoutParams.setMargins(
            dp(startMargin.toFloat()),
            dp(topMargin.toFloat()),
            dp(endMargin.toFloat()),
            dp(bottomMargin.toFloat())
        )
    }

    /**
     * Converts a dp value into px that can be applied on margins, paddings etc
     *
     * @param dp The dp value that will be converted into px
     * @return The converted px value from the dp argument given
     */
    fun dpToPx(dp: Float): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp,
            App.context.resources.displayMetrics
        ).roundToInt()
    }

    @JvmOverloads
    fun showSimpleAlert(
        context: Context,
        title: String?,
        message: String?,
        positive: String? = context.getString(android.R.string.ok),
        negative: String? = null,
        neutral: String? = null,
        listener: DialogInterface.OnClickListener? = null
    ) {
        val builder = MaterialAlertDialogBuilder(context, dialogFullWidthButtonsThemeOverlay)
            .setTitle(title)
            .setMessage(message)
        if (positive != null) {
            builder.setPositiveButton(positive, listener)
        }
        if (negative != null) {
            builder.setNegativeButton(negative, listener)
        }
        if (neutral != null) {
            builder.setNeutralButton(neutral, listener)
        }
        builder.show()
    }

    @JvmStatic
    val dialogFullWidthButtonsThemeOverlay: Int
        get() = R.style.ThemeOverlay_CosmicIde_MaterialAlertDialog_FullWidthButtons

    fun copyToClipboard(text: String?) {
        val clipboard = App.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("", text)
        clipboard.setPrimaryClip(clip)
    }

}