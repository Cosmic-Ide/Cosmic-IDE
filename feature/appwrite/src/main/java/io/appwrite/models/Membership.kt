package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * Membership
 */
data class Membership(
    /**
     * Membership ID.
     */
    @SerializedName("\$id")
    val id: String,

    /**
     * Membership creation date in ISO 8601 format.
     */
    @SerializedName("\$createdAt")
    val createdAt: String,

    /**
     * Membership update date in ISO 8601 format.
     */
    @SerializedName("\$updatedAt")
    val updatedAt: String,

    /**
     * User ID.
     */
    @SerializedName("userId")
    val userId: String,

    /**
     * User name.
     */
    @SerializedName("userName")
    val userName: String,

    /**
     * User email address.
     */
    @SerializedName("userEmail")
    val userEmail: String,

    /**
     * Team ID.
     */
    @SerializedName("teamId")
    val teamId: String,

    /**
     * Team name.
     */
    @SerializedName("teamName")
    val teamName: String,

    /**
     * Date, the user has been invited to join the team in ISO 8601 format.
     */
    @SerializedName("invited")
    val invited: String,

    /**
     * Date, the user has accepted the invitation to join the team in ISO 8601 format.
     */
    @SerializedName("joined")
    val joined: String,

    /**
     * User confirmation status, true if the user has joined the team or false otherwise.
     */
    @SerializedName("confirm")
    val confirm: Boolean,

    /**
     * User list of roles
     */
    @SerializedName("roles")
    val roles: List<Any>,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "\$id" to id as Any,
        "\$createdAt" to createdAt as Any,
        "\$updatedAt" to updatedAt as Any,
        "userId" to userId as Any,
        "userName" to userName as Any,
        "userEmail" to userEmail as Any,
        "teamId" to teamId as Any,
        "teamName" to teamName as Any,
        "invited" to invited as Any,
        "joined" to joined as Any,
        "confirm" to confirm as Any,
        "roles" to roles as Any,
    )

    companion object {

        @Suppress("UNCHECKED_CAST")
        fun from(
            map: Map<String, Any>,
        ) = Membership(
            id = map["\$id"] as String,
            createdAt = map["\$createdAt"] as String,
            updatedAt = map["\$updatedAt"] as String,
            userId = map["userId"] as String,
            userName = map["userName"] as String,
            userEmail = map["userEmail"] as String,
            teamId = map["teamId"] as String,
            teamName = map["teamName"] as String,
            invited = map["invited"] as String,
            joined = map["joined"] as String,
            confirm = map["confirm"] as Boolean,
            roles = map["roles"] as List<Any>,
        )
    }
}
