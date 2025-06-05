/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.genai.errors;

/**
 * IO exception raised in the GenAI SDK.
 */
public final class GenAiIOException extends BaseException {

    /**
     * Creates a new GenAiIoException with the specified message and the original IOException.
     */
    public GenAiIOException(String message, Exception cause) {
        super(message, cause);
    }

    /**
     * Creates a new GenAiIoException with the specified message.
     */
    public GenAiIOException(String message) {
        super(message);
    }

    /**
     * Creates a new GenAiIoException with the specified cause.
     */
    public GenAiIOException(Throwable cause) {
        super(cause.getMessage(), cause);
    }
}
