/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.util

import android.os.Build
import androidx.fragment.app.Fragment
import java.io.File


// Android 14+ doesn't allow loading writable dex files: https://developer.android.com/about/versions/14/behavior-changes-14#safer-dynamic-code-loading
fun Fragment.makeDexReadOnlyIfNeeded(dexFile: File): File {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return dexFile
    }
    val target = requireContext().cacheDir.resolve(dexFile.name)
    if (target.exists()) {
        target.delete()
    }
    target.createNewFile()
    dexFile.inputStream().buffered().use {
        target.writeBytes(it.readBytes())
    }
    target.setReadOnly() // This is required for Android 14+
    return target
}
