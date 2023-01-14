package org.cosmic.ide.ui.model

import android.view.View
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * An action button shown on the main screen.
 */
data class MainScreenAction
@JvmOverloads
constructor(@StringRes val text: Int, @DrawableRes val icon: Int, val onClick: (View) -> Unit = {})