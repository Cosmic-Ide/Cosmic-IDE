package io.appwrite.models

import java.io.File
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Paths

class InputFile private constructor() {

    lateinit var path: String
    lateinit var filename: String
    lateinit var mimeType: String
    lateinit var sourceType: String
    lateinit var data: Any

    companion object {
        fun fromFile(file: File) = InputFile().apply {
            path = file.canonicalPath
            filename = file.name
            mimeType = Files.probeContentType(Paths.get(file.canonicalPath))
                ?: URLConnection.guessContentTypeFromName(filename)
                        ?: ""
            sourceType = "file"
        }

        fun fromPath(path: String): InputFile = fromFile(File(path)).apply {
            sourceType = "path"
        }

        fun fromBytes(bytes: ByteArray, filename: String = "", mimeType: String = "") =
            InputFile().apply {
                this.filename = filename
                this.mimeType = mimeType
                data = bytes
                sourceType = "bytes"
            }
    }
}
