package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Phone
 */
data class Phone(
    /**
     * Phone code.
     */
    @SerializedName("code")
    val code: String,

    /**
     * Country two-character ISO 3166-1 alpha code.
     */
    @SerializedName("countryCode")
    val countryCode: String,

    /**
     * Country name.
     */
    @SerializedName("countryName")
    val countryName: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "code" to code as Any,
        "countryCode" to countryCode as Any,
        "countryName" to countryName as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = Phone(
            code = map["code"] as String,
            countryCode = map["countryCode"] as String,
            countryName = map["countryName"] as String,
        )
    }
}
