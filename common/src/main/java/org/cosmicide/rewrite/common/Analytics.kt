/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.common

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase

object Analytics {
    fun init(context: Context) {
        analytics = Firebase.analytics
    }

    fun logEvent(event: String, bundle: Bundle) {
        analytics.logEvent(event, bundle)
    }

    fun logEvent(event: String, vararg pairs: Pair<String, String>) {
        val bundle = Bundle()
        for (pair in pairs) {
            bundle.putString(pair.first, pair.second)
        }
        logEvent(event, bundle)
    }

    fun setAnalyticsCollectionEnabled(enabled: Boolean) {
        analytics.setAnalyticsCollectionEnabled(enabled)
    }

    lateinit var analytics: FirebaseAnalytics
}