package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * AlgoBcrypt
 */
data class AlgoBcrypt(
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
        ) = AlgoBcrypt(
            type = map["type"] as String,
        )
    }
}
