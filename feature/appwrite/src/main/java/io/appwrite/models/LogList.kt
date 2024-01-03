package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Logs List
 */
data class LogList(
    /**
     * Total number of logs documents that matched your query.
     */
    @SerializedName("total")
    val total: Long,

    /**
     * List of logs.
     */
    @SerializedName("logs")
    val logs: List<Log>,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "total" to total as Any,
        "logs" to logs.map { it.toMap() } as Any,
    )

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun from(
            map: Map<String, Any>,
        ) = LogList(
            total = (map["total"] as Number).toLong(),
            logs = (map["logs"] as List<Map<String, Any>>).map { Log.from(map = it) },
        )
    }
}
