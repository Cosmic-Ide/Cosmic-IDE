package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Currency
 */
data class Currency(
    /**
     * Currency symbol.
     */
    @SerializedName("symbol")
    val symbol: String,

    /**
     * Currency name.
     */
    @SerializedName("name")
    val name: String,

    /**
     * Currency native symbol.
     */
    @SerializedName("symbolNative")
    val symbolNative: String,

    /**
     * Number of decimal digits.
     */
    @SerializedName("decimalDigits")
    val decimalDigits: Long,

    /**
     * Currency digit rounding.
     */
    @SerializedName("rounding")
    val rounding: Double,

    /**
     * Currency code in [ISO 4217-1](http://en.wikipedia.org/wiki/ISO_4217) three-character format.
     */
    @SerializedName("code")
    val code: String,

    /**
     * Currency plural name
     */
    @SerializedName("namePlural")
    val namePlural: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "symbol" to symbol as Any,
        "name" to name as Any,
        "symbolNative" to symbolNative as Any,
        "decimalDigits" to decimalDigits as Any,
        "rounding" to rounding as Any,
        "code" to code as Any,
        "namePlural" to namePlural as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = Currency(
            symbol = map["symbol"] as String,
            name = map["name"] as String,
            symbolNative = map["symbolNative"] as String,
            decimalDigits = (map["decimalDigits"] as Number).toLong(),
            rounding = (map["rounding"] as Number).toDouble(),
            code = map["code"] as String,
            namePlural = map["namePlural"] as String,
        )
    }
}
