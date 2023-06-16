/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File


object FileProvider {
    @JvmStatic
    fun openFileWithExternalApp(context: Context, file: File) {
        val fileUri: Uri =
            FileProvider.getUriForFile(
                context,
                "org.cosmicide.rewrite.fileprovider",
                file
            )

        val intent = Intent(Intent.ACTION_VIEW)
        intent.setDataAndType(fileUri, "*/*")
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        val packageManager: PackageManager = context.packageManager
        val activities =
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

        if (activities.isNotEmpty()) {
            context.startActivity(intent)
        } else {
            // No app can handle the file, handle the scenario as per your requirements
            // For example, show a toast message or fallback to your own file viewer
        }
    }
}