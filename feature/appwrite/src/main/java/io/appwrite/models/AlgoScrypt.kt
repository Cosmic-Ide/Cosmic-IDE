package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * AlgoScrypt
 */
data class AlgoScrypt(
    /**
     * Algo type.
     */
    @SerializedName("type")
    val type: String,

    /**
     * CPU complexity of computed hash.
     */
    @SerializedName("costCpu")
    val costCpu: Long,

    /**
     * Memory complexity of computed hash.
     */
    @SerializedName("costMemory")
    val costMemory: Long,

    /**
     * Parallelization of computed hash.
     */
    @SerializedName("costParallel")
    val costParallel: Long,

    /**
     * Length used to compute hash.
     */
    @SerializedName("length")
    val length: Long,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "type" to type as Any,
        "costCpu" to costCpu as Any,
        "costMemory" to costMemory as Any,
        "costParallel" to costParallel as Any,
        "length" to length as Any,
    )

    companion object {

        fun from(
            map: Map<String, Any>,
        ) = AlgoScrypt(
            type = map["type"] as String,
            costCpu = (map["costCpu"] as Number).toLong(),
            costMemory = (map["costMemory"] as Number).toLong(),
            costParallel = (map["costParallel"] as Number).toLong(),
            length = (map["length"] as Number).toLong(),
        )
    }
}
