package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * LocaleCode
 */
data class LocaleCode(
    /**
     * Locale codes in [ISO 639-1](https://en.wikipedia.org/wiki/List_of_ISO_639-1_codes)
     */
    @SerializedName("code")
    val code: String,

    /**
     * Locale name
     */
    @SerializedName("name")
    val name: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "code" to code as Any,
        "name" to name as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = LocaleCode(
            code = map["code"] as String,
            name = map["name"] as String,
        )
    }
}
