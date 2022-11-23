package org.cosmic.ide.android.exception

class CompilationFailedException : Exception {
    constructor(message: String?, t: Throwable?) : super(message, t)
    constructor(message: String?) : super(message)
    constructor(t: Throwable?) : super(t)
}
