package com.pranav.lib_android.exception;

public class CompilationFailedException extends Exception {

	public CompilationFailedException(Throwable e) {
		super(e);
	}

	public CompilationFailedException(String message) {
		super(message);
	}
}
