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
 * Base exception class for all exceptions specifically originating from the GenAI SDK.
 *
 * <p>This class extends {@link RuntimeException}. The GenAI SDK favors unchecked exceptions
 * to improve developer experience by reducing mandatory {@code try-catch} or {@code throws}
 * clause boilerplate for potentially unrecoverable runtime errors.
 */
class BaseException extends RuntimeException {

    /**
     * Creates a new exception with the specified message.
     */
    BaseException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with the specified message and cause.
     */
    BaseException(String message, Throwable cause) {
        super(message, cause);
    }
}
