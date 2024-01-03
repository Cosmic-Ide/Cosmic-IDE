package io.appwrite.coroutines

import kotlinx.coroutines.Dispatchers
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext

interface Callback<T> {
    fun onComplete(result: T?, error: Throwable?)
}

class CoroutineCallback<T> @JvmOverloads constructor(
    private val callback: Callback<T>,
    override val context: CoroutineContext = Dispatchers.Default
) : Continuation<T> {
    override fun resumeWith(result: Result<T>) {
        callback.onComplete(result.getOrNull(), result.exceptionOrNull())
    }
}