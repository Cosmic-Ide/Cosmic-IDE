/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.util

import android.text.method.ScrollingMovementMethod
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.noties.markwon.Markwon
import io.noties.markwon.core.CorePlugin
import io.noties.markwon.html.HtmlPlugin
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.glide.GlideImagesPlugin
import io.noties.markwon.linkify.LinkifyPlugin
import io.noties.markwon.movement.MovementMethodPlugin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.cosmicide.rewrite.App

object CommonUtils {
    suspend fun showSnackbarError(view: View, text: String, error: Throwable) =
        withContext(Dispatchers.Main) {
            Snackbar.make(
                view,
                text,
                Snackbar.LENGTH_INDEFINITE
            ).setAction("Show error") {
                MaterialAlertDialogBuilder(view.context)
                    .setTitle(error.message)
                    .setMessage(error.stackTraceToString())
                    .setPositiveButton("OK") { _, _ -> }
            }.show()
        }

    fun showError(view: View, text: String, error: Throwable) {
        Snackbar.make(
            view,
            text,
            Snackbar.LENGTH_INDEFINITE
        ).setAction("Show error") {
            MaterialAlertDialogBuilder(view.context)
                .setTitle(error.message)
                .setMessage(error.stackTraceToString())
                .setPositiveButton("OK") { _, _ -> }
        }.show()
    }

    fun getMarkwon() = Markwon
        .builder(App.instance.get()!!.applicationContext)
        .usePlugin(CorePlugin.create())
        .usePlugin(MovementMethodPlugin.create(ScrollingMovementMethod.getInstance()))
        .usePlugin(LinkifyPlugin.create())
        .usePlugin(GlideImagesPlugin.create(App.instance.get()!!.applicationContext))
        .usePlugin(ImagesPlugin.create())
        .usePlugin(HtmlPlugin.create())
        .build()
}