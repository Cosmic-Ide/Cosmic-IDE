package org.cosmicide.ui.editor

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
fun rememberFocusedView(): State<View?> {
    val view = LocalView.current
    val focusedView = remember { mutableStateOf(view.findFocus()) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalFocusChangeListener { _, newFocus ->
            focusedView.value = newFocus
        }
        view.viewTreeObserver.addOnGlobalFocusChangeListener(listener)
        onDispose {
            view.viewTreeObserver.removeOnGlobalFocusChangeListener(listener)
        }
    }

    return focusedView
}
