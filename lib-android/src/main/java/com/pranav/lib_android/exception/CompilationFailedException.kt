package com.pranav.lib_android

class CompilationFailedException: Throwable() {

	constructor(e: Throwable) {
		this(e)
	}

	constructor(message: String) {
		super(message)
	}
}
