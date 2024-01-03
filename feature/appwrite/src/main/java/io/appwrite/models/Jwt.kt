package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * JWT
 */
data class Jwt(
    /**
     * JWT encoded string.
     */
    @SerializedName("jwt")
    val jwt: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "jwt" to jwt as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = Jwt(
            jwt = map["jwt"] as String,
        )
    }
}
