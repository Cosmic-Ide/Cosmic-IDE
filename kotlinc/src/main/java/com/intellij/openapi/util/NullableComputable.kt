package com.intellij.openapi.util

@FunctionalInterface
interface NullableComputable<T> : Computable<T> {
    override fun compute(): T?
}


