package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Country
 */
data class Country(
    /**
     * Country name.
     */
    @SerializedName("name")
    val name: String,

    /**
     * Country two-character ISO 3166-1 alpha code.
     */
    @SerializedName("code")
    val code: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "name" to name as Any,
        "code" to code as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = Country(
            name = map["name"] as String,
            code = map["code"] as String,
        )
    }
}
