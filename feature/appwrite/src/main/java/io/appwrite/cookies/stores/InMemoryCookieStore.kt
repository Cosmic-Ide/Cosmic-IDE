package io.appwrite.cookies.stores

import android.os.Build
import android.util.Log
import java.net.CookieStore
import java.net.HttpCookie
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.locks.ReentrantLock

open class InMemoryCookieStore(private val name: String) : CookieStore {

    internal val uriIndex = mutableMapOf<URI, MutableList<HttpCookie>>()
    private val lock = ReentrantLock(false)

    override fun removeAll(): Boolean {
        lock.lock()

        return try {
            uriIndex.clear()
            uriIndex.isEmpty()
        } finally {
            lock.unlock()
        }
    }

    override fun add(uri: URI?, cookie: HttpCookie?) {
        if (cookie == null) {
            Log.i(
                javaClass.simpleName,
                "tried to add null cookie in cookie store named $name. Doing nothing."
            )
            return
        }

        if (uri == null) {
            Log.i(
                javaClass.simpleName,
                "tried to add null URI in cookie store named $name. Doing nothing."
            )
            return
        }

        lock.lock()
        try {
            addIndex(getEffectiveURI(uri), cookie)
        } finally {
            lock.unlock()
        }
    }

    override fun getCookies(): List<HttpCookie> {
        val cookies = mutableListOf<HttpCookie>()

        lock.lock()
        try {
            for (list in uriIndex.values) {
                val it = list.iterator()
                while (it.hasNext()) {
                    val cookie = it.next()
                    if (cookie.hasExpired()) {
                        it.remove()
                    } else if (!cookies.contains(cookie)) {
                        cookies.add(cookie)
                    }
                }
            }
        } finally {
            lock.unlock()
        }

        return cookies
    }

    override fun getURIs(): List<URI> {
        val uris = mutableListOf<URI>()

        lock.lock()
        return try {
            uriIndex.keys.toList()
        } finally {
            uris.addAll(uriIndex.keys)
            lock.unlock()
        }
    }

    override fun remove(uri: URI?, cookie: HttpCookie?): Boolean {
        if (cookie == null) {
            Log.i(
                javaClass.simpleName,
                "tried to remove null cookie from cookie store named $name. Doing nothing."
            )
            return true
        }

        if (uri == null) {
            Log.i(
                javaClass.simpleName,
                "tried to remove null URI from cookie store named $name. Doing nothing."
            )
            return true
        }

        lock.lock()

        return try {
            val lintedURI = getEffectiveURI(uri)

            if (uriIndex[lintedURI] == null) {
                false
            } else {
                val cookies = uriIndex[lintedURI]
                cookies?.remove(cookie) ?: false
            }
        } finally {
            lock.unlock()
        }
    }

    override fun get(uri: URI?): List<HttpCookie> {
        if (uri == null) {
            Log.i(
                javaClass.simpleName,
                "getting cookies from cookie store named $name for null URI results in empty list"
            )
            return emptyList()
        }

        val cookies = arrayListOf<HttpCookie>()

        lock.lock()
        try {
            uri.host?.let { cookies.addAll(getInternal1(it)) }
            val internal2 = getInternal2(getEffectiveURI(uri)).filter { !cookies.contains(it) }
            cookies.addAll(internal2)
        } finally {
            lock.unlock()
        }

        return cookies
    }

    internal fun getEffectiveURI(uri: URI): URI {
        return try {
            URI(uri.scheme ?: "http", uri.host, null, null, null)
        } catch (ignored: URISyntaxException) {
            uri
        }
    }

    private fun addIndex(index: URI, cookie: HttpCookie) {
        val cookies = uriIndex[index]

        if (cookies != null) {
            cookies.remove(cookie)
            cookies.add(cookie)
        } else {
            val newCookies = mutableListOf<HttpCookie>()
            newCookies.add(cookie)
            uriIndex[index] = newCookies
        }
    }

    private fun netscapeDomainMatches(domain: String?, host: String?): Boolean {
        if (domain == null || host == null) {
            return false
        }

        // If there's no embedded dot in domain and domain is not .local
        val isLocalDomain = ".local".equals(domain, ignoreCase = true)
        var embeddedDotInDomain = domain.indexOf('.')
        if (embeddedDotInDomain == 0) {
            embeddedDotInDomain = domain.indexOf('.', 1)
        }
        if (!isLocalDomain && (embeddedDotInDomain == -1 || embeddedDotInDomain == domain.length - 1)) {
            return false
        }

        // If the host name contains no dot and the domain name is .local
        val firstDotInHost = host.indexOf('.')
        if (firstDotInHost == -1 && isLocalDomain) {
            return true
        }

        val domainLength = domain.length
        val lengthDiff = host.length - domainLength
        if (lengthDiff == 0) {
            // If the host name and the domain name are just string-compare euqal
            return host.equals(domain, ignoreCase = true)
        } else if (lengthDiff > 0) {
            // need to check H & D component
            val D = host.substring(lengthDiff)

            // Android-changed: b/26456024 targetSdkVersion based compatibility for domain matching
            // Android M and earlier: Cookies with domain "foo.com" would not match "bar.foo.com".
            // The RFC dictates that the user agent must treat those domains as if they had a
            // leading period and must therefore match "bar.foo.com".
            return if (Build.VERSION.SDK_INT <= 23 && !domain.startsWith(".")) {
                false
            } else D.equals(domain, ignoreCase = true)
        } else if (lengthDiff == -1) {
            // if domain is actually .host
            return domain[0] == '.' && host.equals(domain.substring(1), ignoreCase = true)
        }

        return false
    }

    private fun getInternal1(host: String): List<HttpCookie> {
        // BEGIN Android-changed: b/25897688 InMemoryCookieStore ignores scheme (http/https)
        // Use a separate list to handle cookies that need to be removed so
        // that there is no conflict with iterators.
        val toRemove = mutableListOf<HttpCookie>()
        val cookies = mutableListOf<HttpCookie>()

        for ((_, lst) in uriIndex) {
            for (c in lst) {
                val domain = c.domain
                if (c.version == 0 && netscapeDomainMatches(
                        domain,
                        host
                    ) || c.version == 1 && HttpCookie.domainMatches(domain, host)
                ) {
                    if (!c.hasExpired()) {
                        if (!cookies.contains(c)) {
                            cookies.add(c)
                        }
                    } else {
                        toRemove.add(c)
                    }
                }
            }
            // Clean up the cookies that need to be removed
            for (c in toRemove) {
                lst.remove(c)
            }
            toRemove.clear()
        }
        return cookies
    }

    private fun getInternal2(comparator: URI): List<HttpCookie> {
        // BEGIN Android-changed: b/25897688 InMemoryCookieStore ignores scheme (http/https)
        // Removed cookieJar
        val cookies = mutableListOf<HttpCookie>()

        for (index in uriIndex.keys) {
            if (index === comparator || comparator.compareTo(index) == 0) {
                val indexedCookies = uriIndex[index]
                // Check the list of cookies associated with this domain
                if (indexedCookies != null) {
                    val it = indexedCookies.iterator()
                    while (it.hasNext()) {
                        val ck = it.next()
                        if (!ck.hasExpired()) {
                            if (!cookies.contains(ck))
                                cookies.add(ck)
                        } else {
                            it.remove()
                        }
                    }
                }
            }
        }

        return cookies
    }
}
