package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Sessions List
 */
data class SessionList(
    /**
     * Total number of sessions documents that matched your query.
     */
    @SerializedName("total")
    val total: Long,

    /**
     * List of sessions.
     */
    @SerializedName("sessions")
    val sessions: List<Session>,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "total" to total as Any,
        "sessions" to sessions.map { it.toMap() } as Any,
    )

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun from(
            map: Map<String, Any>,
        ) = SessionList(
            total = (map["total"] as Number).toLong(),
            sessions = (map["sessions"] as List<Map<String, Any>>).map { Session.from(map = it) },
        )
    }
}
