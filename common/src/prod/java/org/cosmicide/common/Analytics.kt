/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Cosmic IDE. If not, see <https://www.gnu.org/licenses/>.
 */

package org.cosmicide.common

import android.content.Context
import android.os.Build
import android.util.Log
import io.appwrite.Client
import io.appwrite.Permission
import io.appwrite.Role
import io.appwrite.exceptions.AppwriteException
import io.appwrite.extensions.toJson
import io.appwrite.services.Databases
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object Analytics {

    private val documentId = Build.USER + "-" + Build.MODEL

    fun init(context: Context) {
        client = Client(context)
            .setEndpoint("https://cloud.appwrite.io/v1")
            .setProject("64f212248f1c810b1152")
            .setSelfSigned(true)
    }

    private var isAnalyticsCollectionEnabled = true

    lateinit var client: Client
    val databases by lazy { Databases(client) }

    private val scope = CoroutineScope(Dispatchers.IO)

    fun logEvent(event: String, value: Any) {
        if (!isAnalyticsCollectionEnabled) return
        Log.d("Analytics", "Logging event: $event")
        scope.launch {
            try {
                if (databases.listDocuments(
                        "stats",
                        "clients"
                    ).documents.any { it.id == documentId }
                ) {
                    databases.updateDocument(
                        databaseId = "stats",
                        collectionId = "clients",
                        documentId = documentId,
                        data = mapOf(
                            event to value
                        ),
                        permissions = listOf(
                            Permission.update(Role.guests()),
                        )
                    )
                    return@launch
                }
                databases.createDocument(
                    databaseId = "stats",
                    collectionId = "clients",
                    documentId = documentId,
                    data = mapOf(
                        event to value
                    ),
                    permissions = listOf(
                        Permission.write(Role.guests()),
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @JvmStatic
    fun logEvent(event: String, vararg pairs: Pair<String, String>) {
        if (!isAnalyticsCollectionEnabled) return
        Log.d("Analytics", "Logging event: $event")
        val log = pairs.joinToString(", ") { "${it.first}=${it.second}" }
        scope.launch {
            try {
                if (databases.listDocuments(
                        "stats",
                        "clients"
                    ).documents.any { it.id == documentId }
                ) {
                    databases.updateDocument(
                        databaseId = "stats",
                        collectionId = "clients",
                        documentId = documentId,
                        data = mapOf(
                            event to pairs.toMap()
                        ),
                        permissions = listOf(
                            Permission.update(Role.guests()),
                        )
                    )
                    return@launch
                }
                val doc = databases.createDocument(
                    databaseId = "stats",
                    collectionId = "clients",
                    documentId = documentId,
                    data = mapOf(
                        event to log
                    ),
                    permissions = listOf(
                        Permission.write(Role.guests()),
                    )
                )
                Log.d("Analytics", "Logged event: ${doc.toJson()}")
            } catch (e: Exception) {
                if (e !is AppwriteException) {
                    e.printStackTrace()
                }
            }
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
