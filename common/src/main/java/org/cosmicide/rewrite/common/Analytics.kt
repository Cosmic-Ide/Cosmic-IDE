/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.rewrite.common

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.extensions.toJson
import io.appwrite.services.Account
import io.appwrite.services.Databases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Analytics {

    private val account by lazy { Account(client) }

    suspend fun init(context: Context) {
        Log.d("Analytics", "Initializing")
        try {
            client = Client(context)
                .setEndpoint("https://cloud.appwrite.io/v1")
                .setProject("64f212248f1c810b1152")
                .setSelfSigned(true)

            if (account.listSessions().sessions.isNotEmpty()) {
                Log.d("Analytics", "User already exists")
                return
            }
            Log.d("Analytics", "Creating user ${Build.MODEL}")
            account.create(
                userId = ID.unique(),
                email = Build.MODEL + "@cosmicide.org",
                password = "password",
                name = Build.USER
            )
            Log.d("Analytics", "Created session ${account.get().toJson()}")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private var isAnalyticsCollectionEnabled = true

    lateinit var client: Client
    val databases by lazy { Databases(client) }

    private val scope = CoroutineScope(Dispatchers.IO)

    @JvmStatic
    fun logEvent(event: String, bundle: Bundle) {
        if (!isAnalyticsCollectionEnabled) return
        Log.d("Analytics", "Logging event: $bundle")
        scope.launch {

            try {
                account.createVerification("https://localhost:8080")
                val doc = databases.createDocument(
                    databaseId = "stats",
                    collectionId = "users",
                    documentId = "doc-1",
                    data = bundle,
                    permissions = listOf(
                        Permission.write(Role.user(Build.MODEL)),
                        Permission.read(Role.user(Build.MODEL))
                    )
                )
                Log.d("Analytics", "Logged event: ${doc.toJson()}")
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun logEvent(event: String, vararg pairs: Pair<String, String>) {
        try {
            val bundle = Bundle()
            for (pair in pairs) {
                bundle.putString(pair.first, pair.second)
            }
            logEvent(event, bundle)
        } catch (e: Exception) {
            e.printStackTrace()
        }
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