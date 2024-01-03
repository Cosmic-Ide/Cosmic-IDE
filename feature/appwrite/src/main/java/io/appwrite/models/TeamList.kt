package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Teams List
 */
data class TeamList<T>(
    /**
     * Total number of teams documents that matched your query.
     */
    @SerializedName("total")
    val total: Long,

    /**
     * List of teams.
     */
    @SerializedName("teams")
    val teams: List<Team<T>>,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "total" to total as Any,
        "teams" to teams.map { it.toMap() } as Any,
    )

    companion object {
        operator fun invoke(
            total: Long,
            teams: List<Team<Map<String, Any>>>,
        ) = TeamList(
            total,
            teams,
        )

        @Suppress("UNCHECKED_CAST")
        fun <T> from(
            map: Map<String, Any>,
            nestedType: Class<T>
        ) = TeamList(
            total = (map["total"] as Number).toLong(),
            teams = (map["teams"] as List<Map<String, Any>>).map {
                Team.from(
                    map = it,
                    nestedType
                )
            },
        )
    }
}
