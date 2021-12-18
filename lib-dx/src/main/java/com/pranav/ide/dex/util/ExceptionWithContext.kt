/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pranav.ide.dex.util

import java.io.PrintStream
import java.io.PrintWriter

/**
 * Exception which carries around structured context.
 */
open class ExceptionWithContext @JvmOverloads constructor(
    message: String?,
    cause: Throwable? = null
) : RuntimeException(
    message ?: cause?.message,
    cause
) {
    /** `non-null;` human-oriented context of the exception  */
    private var context: StringBuffer? = null

    /**
     * Constructs an instance.
     *
     * @param cause `null-ok;` exception that caused this one
     */
    constructor(cause: Throwable?) : this(null, cause)

    /** {@inheritDoc}  */
    override fun printStackTrace(out: PrintStream) {
        super.printStackTrace(out)
        out.println(context)
    }

    /** {@inheritDoc}  */
    override fun printStackTrace(out: PrintWriter) {
        super.printStackTrace(out)
        out.println(context)
    }

    /**
     * Adds a line of context to this instance.
     *
     * @param str `non-null;` new context
     */
    fun addContext(str: String?) {
        if (str == null) {
            throw NullPointerException("str == null")
        }
        context!!.append(str)
        if (!str.endsWith("\n")) {
            context!!.append('\n')
        }
    }

    /**
     * Gets the context.
     *
     * @return `non-null;` the context
     */
    fun getContext(): String {
        return context.toString()
    }

    /**
     * Prints the message and context.
     *
     * @param out `non-null;` where to print to
     */
    fun printContext(out: PrintStream) {
        out.println(message)
        out.print(context)
    }

    companion object {
        /**
         * Augments the given exception with the given context, and return the
         * result. The result is either the given exception if it was an
         * [ExceptionWithContext], or a newly-constructed exception if it
         * was not.
         *
         * @param ex `non-null;` the exception to augment
         * @param str `non-null;` context to add
         * @return `non-null;` an appropriate instance
         */
        @JvmStatic
        fun withContext(ex: Throwable?, str: String?): ExceptionWithContext {
            val ewc: ExceptionWithContext = if (ex is ExceptionWithContext) {
                ex
            } else {
                ExceptionWithContext(ex)
            }
            ewc.addContext(str)
            return ewc
        }
    }
    /**
     * Constructs an instance.
     *
     * @param message human-oriented message
     * @param cause `null-ok;` exception that caused this one
     */
    /**
     * Constructs an instance.
     *
     * @param message human-oriented message
     */
    init {
        if (cause is ExceptionWithContext) {
            val ctx = cause.context.toString()
            context = StringBuffer(ctx.length + 200)
            context!!.append(ctx)
        } else {
            context = StringBuffer(200)
        }
    }
}