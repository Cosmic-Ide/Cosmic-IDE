package org.cosmic.ide.util

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return // Fragment not attached to an Activity
    activity?.runOnUiThread(action)
}

fun Fragment?.setSupportActionBar(toolbar: Toolbar) {
    this ?: return
    if (activity is AppCompatActivity) {
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
    } else {
        return // Parent activity does not extends on AppCompatActivity
    }
}