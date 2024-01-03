package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * AlgoSHA
 */
data class AlgoSha(
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
        ) = AlgoSha(
            type = map["type"] as String,
        )
    }
}
