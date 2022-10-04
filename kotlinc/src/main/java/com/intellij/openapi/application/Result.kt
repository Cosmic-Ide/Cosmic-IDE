package com.intellij.openapi.application

class Result<T> {

    protected var myResult: T

    fun setResult(result: T) {
        myResult = result
    }
}