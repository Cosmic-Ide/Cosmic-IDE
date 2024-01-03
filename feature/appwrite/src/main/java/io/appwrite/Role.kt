package io.appwrite

/**
 * Helper class to generate role strings for [Permission].
 */
class Role {
    companion object {

        /**
         * Grants access to anyone.
         *
         * This includes authenticated and unauthenticated users.
         */
        fun any(): String = "any"

        /**
         * Grants access to a specific user by user ID.
         *
         * You can optionally pass verified or unverified for
         * [status] to target specific types of users.
         */
        fun user(id: String, status: String = ""): String = if (status.isEmpty()) {
            "user:$id"
        } else {
            "user:$id/$status"
        }

        /**
         * Grants access to any authenticated or anonymous user.
         *
         * You can optionally pass verified or unverified for
         * [status] to target specific types of users.
         */
        fun users(status: String = ""): String = if (status.isEmpty()) {
            "users"
        } else {
            "users/$status"
        }

        /**
         * Grants access to any guest user without a session.
         *
         * Authenticated users don't have access to this role.
         */
        fun guests(): String = "guests"

        /**
         * Grants access to a team by team ID.
         *
         * You can optionally pass a role for [role] to target
         * team members with the specified role.
         */
        fun team(id: String, role: String = ""): String = if (role.isEmpty()) {
            "team:$id"
        } else {
            "team:$id/$role"
        }

        /**
         * Grants access to a specific member of a team.
         *
         * When the member is removed from the team, they will
         * no longer have access.
         */
        fun member(id: String): String = "member:$id"

        /**
         * Grants access to a user with the specified label.
         */
        fun label(name: String): String = "label:$name"
    }
}
