package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * AlgoPHPass
 */
data class AlgoPhpass(
    /**
     * Algo type.
     */
    @SerializedName("type")
    val type: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "type" to type as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = AlgoPhpass(
            type = map["type"] as String,
        )
    }
}
