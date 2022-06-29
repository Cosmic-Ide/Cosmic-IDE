package com.pranav.android.exception

class CompilationFailedException(message: String) : Exception(message) {
    constructor(e: Throwable): super(e)
}
