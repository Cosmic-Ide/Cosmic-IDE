package io.appwrite

class ID {
    companion object {
        fun custom(id: String): String = id
        fun unique(): String = "unique()"
    }
}
