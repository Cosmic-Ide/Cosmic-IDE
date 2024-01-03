package io.appwrite.cookies

import android.os.Build
import java.net.HttpCookie

data class InternalCookie(
    val comment: String?,
    val commentURL: String?,
    val discard: Boolean?,
    val domain: String,
    val maxAge: Long?,
    val name: String,
    val path: String?,
    val portlist: String?,
    val secure: Boolean?,
    val value: String,
    val version: Int?,
    var httpOnly: Boolean? = null
) {
    constructor(cookie: HttpCookie) : this(
        cookie.comment,
        cookie.commentURL,
        cookie.discard,
        cookie.domain,
        cookie.maxAge,
        cookie.name,
        cookie.path,
        cookie.portlist,
        cookie.secure,
        cookie.value,
        cookie.version
    )

    fun toHttpCookie() = HttpCookie(name, value).apply {
        comment = this@InternalCookie.comment
        commentURL = this@InternalCookie.commentURL
        discard = this@InternalCookie.discard == true
        domain = this@InternalCookie.domain
        maxAge = this@InternalCookie.maxAge ?: 0
        path = this@InternalCookie.path
        portlist = this@InternalCookie.portlist
        secure = this@InternalCookie.secure == true
        version = this@InternalCookie.version ?: 0

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            isHttpOnly = (this@InternalCookie.httpOnly == true)
        }
    }
}