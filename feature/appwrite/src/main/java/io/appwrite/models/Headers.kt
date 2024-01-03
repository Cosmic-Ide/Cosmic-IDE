package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Headers
 */
data class Headers(
    /**
     * Header name.
     */
    @SerializedName("name")
    val name: String,

    /**
     * Header value.
     */
    @SerializedName("value")
    val value: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name as Any,
        "value" to value as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = Headers(
            name = map["name"] as String,
            value = map["value"] as String,
        )
    }
}
