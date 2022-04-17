package com.pranav.lib_android.exception

class CompilationFailedException: Exception() {
  constructor(e?: Throwable): this(e)
  constructor(message?: String): this(message)
}
