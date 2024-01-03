package io.appwrite

class Permission {
    companion object {
        fun read(role: String): String {
            return "read(\"${role}\")"
        }

        fun write(role: String): String {
            return "write(\"${role}\")"
        }

        fun create(role: String): String {
            return "create(\"${role}\")"
        }

        fun update(role: String): String {
            return "update(\"${role}\")"
        }

        fun delete(role: String): String {
            return "delete(\"${role}\")"
        }
    }
}
