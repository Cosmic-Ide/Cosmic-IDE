package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Countries List
 */
data class CountryList(
    /**
     * Total number of countries documents that matched your query.
     */
    @SerializedName("total")
    val total: Long,

    /**
     * List of countries.
     */
    @SerializedName("countries")
    val countries: List<Country>,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "total" to total as Any,
        "countries" to countries.map { it.toMap() } as Any,
    )

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun from(
            map: Map<String, Any>,
        ) = CountryList(
            total = (map["total"] as Number).toLong(),
            countries = (map["countries"] as List<Map<String, Any>>).map { Country.from(map = it) },
        )
    }
}
