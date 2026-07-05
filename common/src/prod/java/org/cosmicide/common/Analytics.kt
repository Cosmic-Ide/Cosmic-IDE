/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.common

import android.content.Context

object Analytics {
    fun init(context: Context) {
    }

    private var isAnalyticsCollectionEnabled = true

    fun logEvent(event: String, value: Any) {
        if (!isAnalyticsCollectionEnabled) return

        // log event
    }

    @JvmStatic
    fun logEvent(event: String, vararg pairs: Pair<String, String>) {
        if (!isAnalyticsCollectionEnabled) return

        // log event
    }

    @JvmStatic
    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        try {
            isAnalyticsCollectionEnabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
