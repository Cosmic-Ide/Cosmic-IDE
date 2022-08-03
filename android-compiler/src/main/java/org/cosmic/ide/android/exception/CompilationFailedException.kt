package org.cosmic.ide.android.exception

class CompilationFailedException : Exception {
    constructor(message: String?) : super(message)
    constructor(e: Throwable?): super(e)
}
