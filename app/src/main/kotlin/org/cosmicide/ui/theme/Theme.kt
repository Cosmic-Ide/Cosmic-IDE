package org.cosmicide.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun IDETheme(
    darkTheme: Boolean = isDeviceInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> darkColorScheme()
        else -> expressiveLightColorScheme()
    }

    val shapes = Shapes(largeIncreased = RoundedCornerShape(36.0.dp))

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        shapes = shapes,
        content = content
    )
}

@Composable
fun isDeviceInDarkTheme(): Boolean {
    val context = LocalContext.current
    val localNightMode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK
    val contextNightMode =
        context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK

    if (
        localNightMode == Configuration.UI_MODE_NIGHT_YES ||
        contextNightMode == Configuration.UI_MODE_NIGHT_YES
    ) {
        return true
    }

    val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return uiModeManager.nightMode == UiModeManager.MODE_NIGHT_YES
}
