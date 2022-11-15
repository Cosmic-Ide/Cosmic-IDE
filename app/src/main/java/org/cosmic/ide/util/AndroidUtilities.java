package org.cosmic.ide.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import org.cosmic.ide.App;
import org.cosmic.ide.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressWarnings("unused")
public class AndroidUtilities {

    public static void showToast(String message) {
        Toast.makeText(App.context, message, Toast.LENGTH_LONG).show();
    }

    public static void showToast(@StringRes int id) {
        Toast.makeText(App.context, id, Toast.LENGTH_SHORT).show();
    }
    
    public static int dp(float px) {
        return Math.round(App.context
                .getResources().getDisplayMetrics().density * px);
    }

    public static void setMargins(View view, int startMargin, int topMargin, int endMargin, int bottomMargin) {
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        layoutParams.setMargins(dp(startMargin), dp(topMargin), dp(endMargin), dp(bottomMargin));
    }

    /**
     * Converts a dp value into px that can be applied on margins, paddings etc
     * @param dp The dp value that will be converted into px
     * @return The converted px value from the dp argument given
     */
    public static int dpToPx(float dp) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dp, App.context.getResources().getDisplayMetrics()));
    }

    public static void hideKeyboard(View view) {
        if (view == null) {
            return;
        }
        try {
            var imm = (InputMethodManager) view.getContext().getSystemService(Activity.INPUT_METHOD_SERVICE);
            if (!imm.isActive()) {
                return;
            }
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        } catch (Exception e) {
            Log.d("AndroidUtilities", "Failed to close keyboard " + e.getLocalizedMessage());
        }
    }

    public static void showSimpleAlert(Context context, @StringRes int title, @StringRes int message) {
        showSimpleAlert(context, context.getString(title), context.getString(message));
    }

    public static void showSimpleAlert(@NonNull Context context, @StringRes int title, String message) {
        showSimpleAlert(context, context.getString(title), message);
    }

    public static void showSimpleAlert(Context context, String title, String message) {
        showSimpleAlert(context, title, message, context.getString(android.R.string.ok), null, null, null);
    }

    public static void showSimpleAlert(Context context, String title, String message, String positive, String negative, DialogInterface.OnClickListener listener) {
        showSimpleAlert(context, title, message, positive, negative, null, listener);
    }

    public static void showSimpleAlert(Context context, String title, String message, String positive, String negative, String neutral, DialogInterface.OnClickListener listener) {
        var builder = new MaterialAlertDialogBuilder(context, getDialogFullWidthButtonsThemeOverlay())
                .setTitle(title)
                .setMessage(message);
        if (positive != null) {
            builder.setPositiveButton(positive, listener);
        }
        if (negative != null) {
            builder.setNegativeButton(negative, listener);
        }
        if (neutral != null) {
            builder.setNeutralButton(neutral, listener);
        }
        builder.show();
    }

    public static int getDialogFullWidthButtonsThemeOverlay() {
        return R.style.ThemeOverlay_CosmicIde_MaterialAlertDialog_FullWidthButtons;
    }

    public static void copyToClipboard(String text) {
        var clipboard = (ClipboardManager) App.context
                .getSystemService(Context.CLIPBOARD_SERVICE);

        var clip = ClipData.newPlainText("", text);
        clipboard.setPrimaryClip(clip);
    }

    @Nullable
    public static CharSequence getPrimaryClip() {
        var clipboard = (ClipboardManager) App.context
                .getSystemService(Context.CLIPBOARD_SERVICE);
        var primaryClip = clipboard.getPrimaryClip();
        if (primaryClip != null) {
            if (primaryClip.getItemCount() >= 1) {
                return primaryClip.getItemAt(0).getText();
            }
        }
        return null;
    }

    public static void copyToClipboard(String text, boolean showToast) {
        copyToClipboard(text);

        if (showToast) {
            showToast("Copied \"" + text + "\" to clipboard");
        }
    }
    
    public static int getHeight(ViewGroup viewGroup) {
        int height = 0;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            var view = viewGroup.getChildAt(i);
            height += view.getMeasuredHeight();
        }
        
        return height;
    }

    public static int getRowCount(int itemWidth) {
        var displayMetrics = App.context
                .getResources().getDisplayMetrics();

        return (displayMetrics.widthPixels / itemWidth);
    }
}
