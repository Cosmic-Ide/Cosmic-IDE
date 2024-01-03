package io.appwrite.extensions

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

suspend fun <T> Collection<T>.forEachAsync(
    callback: suspend (T) -> Unit
) = withContext(IO) {
    map { async { callback.invoke(it) } }.awaitAll()
}