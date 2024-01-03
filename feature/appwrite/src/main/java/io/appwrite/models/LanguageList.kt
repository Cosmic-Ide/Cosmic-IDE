package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Languages List
 */
data class LanguageList(
    /**
     * Total number of languages documents that matched your query.
     */
    @SerializedName("total")
    val total: Long,

    /**
     * List of languages.
     */
    @SerializedName("languages")
    val languages: List<Language>,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "total" to total as Any,
        "languages" to languages.map { it.toMap() } as Any,
    )

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun from(
            map: Map<String, Any>,
        ) = LanguageList(
            total = (map["total"] as Number).toLong(),
            languages = (map["languages"] as List<Map<String, Any>>).map { Language.from(map = it) },
        )
    }
}
