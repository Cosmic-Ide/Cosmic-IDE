package com.pranav.lib_android.exception

class CompilationFailedException: Throwable {
  constructor(e: Throwable?): super(e)

  constructor(message: String?): super(message)
}
