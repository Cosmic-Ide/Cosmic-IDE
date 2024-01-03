package io.appwrite.models

import com.google.gson.annotations.SerializedName
import io.appwrite.extensions.jsonCast

/**
 * Preferences
 */
data class Preferences<T>(
    /**
     * Additional properties
     */
    @SerializedName("data")
    val data: T
) {
    fun toMap(): Map<String, Any> = mapOf(
        "data" to data!!.jsonCast(to = Map::class.java)
    )

    companion object {
        operator fun invoke(
            data: Map<String, Any>
        ) = Preferences(
            data
        )

        fun <T> from(
            map: Map<String, Any>,
            nestedType: Class<T>
        ) = Preferences<T>(
            data = map.jsonCast(to = nestedType)
        )
    }
}
