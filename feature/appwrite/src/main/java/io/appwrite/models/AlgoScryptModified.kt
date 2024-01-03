package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * AlgoScryptModified
 */
data class AlgoScryptModified(
    /**
     * Algo type.
     */
    @SerializedName("type")
    val type: String,

    /**
     * Salt used to compute hash.
     */
    @SerializedName("salt")
    val salt: String,

    /**
     * Separator used to compute hash.
     */
    @SerializedName("saltSeparator")
    val saltSeparator: String,

    /**
     * Key used to compute hash.
     */
    @SerializedName("signerKey")
    val signerKey: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "type" to type as Any,
        "salt" to salt as Any,
        "saltSeparator" to saltSeparator as Any,
        "signerKey" to signerKey as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = AlgoScryptModified(
            type = map["type"] as String,
            salt = map["salt"] as String,
            saltSeparator = map["saltSeparator"] as String,
            signerKey = map["signerKey"] as String,
        )
    }
}
