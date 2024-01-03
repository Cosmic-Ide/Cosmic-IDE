package io.appwrite.models

import com.google.gson.annotations.SerializedName

/**
 * User
 */
data class User<T>(
    /**
     * User ID.
     */
    @SerializedName("\$id")
    val id: String,

    /**
     * User creation date in ISO 8601 format.
     */
    @SerializedName("\$createdAt")
    val createdAt: String,

    /**
     * User update date in ISO 8601 format.
     */
    @SerializedName("\$updatedAt")
    val updatedAt: String,

    /**
     * User name.
     */
    @SerializedName("name")
    val name: String,

    /**
     * Hashed user password.
     */
    @SerializedName("password")
    var password: String?,

    /**
     * Password hashing algorithm.
     */
    @SerializedName("hash")
    var hash: String?,

    /**
     * Password hashing algorithm configuration.
     */
    @SerializedName("hashOptions")
    var hashOptions: Any?,

    /**
     * User registration date in ISO 8601 format.
     */
    @SerializedName("registration")
    val registration: String,

    /**
     * User status. Pass `true` for enabled and `false` for disabled.
     */
    @SerializedName("status")
    val status: Boolean,

    /**
     * Labels for the user.
     */
    @SerializedName("labels")
    val labels: List<Any>,

    /**
     * Password update time in ISO 8601 format.
     */
    @SerializedName("passwordUpdate")
    val passwordUpdate: String,

    /**
     * User email address.
     */
    @SerializedName("email")
    val email: String,

    /**
     * User phone number in E.164 format.
     */
    @SerializedName("phone")
    val phone: String,

    /**
     * Email verification status.
     */
    @SerializedName("emailVerification")
    val emailVerification: Boolean,

    /**
     * Phone verification status.
     */
    @SerializedName("phoneVerification")
    val phoneVerification: Boolean,

    /**
     * User preferences as a key-value object
     */
    @SerializedName("prefs")
    val prefs: Preferences<T>,

    /**
     * Most recent access date in ISO 8601 format. This attribute is only updated again after 24 hours.
     */
    @SerializedName("accessedAt")
    val accessedAt: String,

    ) {
    fun toMap(): Map<String, Any> = mapOf(
        "\$id" to id as Any,
        "\$createdAt" to createdAt as Any,
        "\$updatedAt" to updatedAt as Any,
        "name" to name as Any,
        "password" to password as Any,
        "hash" to hash as Any,
        "hashOptions" to hashOptions as Any,
        "registration" to registration as Any,
        "status" to status as Any,
        "labels" to labels as Any,
        "passwordUpdate" to passwordUpdate as Any,
        "email" to email as Any,
        "phone" to phone as Any,
        "emailVerification" to emailVerification as Any,
        "phoneVerification" to phoneVerification as Any,
        "prefs" to prefs.toMap() as Any,
        "accessedAt" to accessedAt as Any,
    )

    companion object {
        operator fun invoke(
            id: String,
            createdAt: String,
            updatedAt: String,
            name: String,
            password: String?,
            hash: String?,
            hashOptions: Any?,
            registration: String,
            status: Boolean,
            labels: List<Any>,
            passwordUpdate: String,
            email: String,
            phone: String,
            emailVerification: Boolean,
            phoneVerification: Boolean,
            prefs: Preferences<Map<String, Any>>,
            accessedAt: String,
        ) = User(
            id,
            createdAt,
            updatedAt,
            name,
            password,
            hash,
            hashOptions,
            registration,
            status,
            labels,
            passwordUpdate,
            email,
            phone,
            emailVerification,
            phoneVerification,
            prefs,
            accessedAt,
        )

        @Suppress("UNCHECKED_CAST")
        fun <T> from(
            map: Map<String, Any>,
            nestedType: Class<T>
        ) = User(
            id = map["\$id"] as String,
            createdAt = map["\$createdAt"] as String,
            updatedAt = map["\$updatedAt"] as String,
            name = map["name"] as String,
            password = map["password"] as? String?,
            hash = map["hash"] as? String?,
            hashOptions = map["hashOptions"],
            registration = map["registration"] as String,
            status = map["status"] as Boolean,
            labels = map["labels"] as List<Any>,
            passwordUpdate = map["passwordUpdate"] as String,
            email = map["email"] as String,
            phone = map["phone"] as String,
            emailVerification = map["emailVerification"] as Boolean,
            phoneVerification = map["phoneVerification"] as Boolean,
            prefs = Preferences.from(map = map["prefs"] as Map<String, Any>, nestedType),
            accessedAt = map["accessedAt"] as String,
        )
    }
}
