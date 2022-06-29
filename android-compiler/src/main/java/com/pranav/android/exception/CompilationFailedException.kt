package com.pranav.android.exception

class CompilationFailedException : Exception {
    constructor(message: String?) : super(message)
    constructor(e: Throwable?): super(e)
}
