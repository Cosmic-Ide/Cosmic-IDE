/*******************************************************************************
 * Copyright (c) Contributors to the Eclipse Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0 
 *******************************************************************************/

package org.osgi.service.prefs;

/**
 * Thrown to indicate that a preferences operation could not complete because of
 * a failure in the backing store, or a failure to contact the backing store.
 * 
 * @author $Id$
 */
public class BackingStoreException extends Exception {
	static final long	serialVersionUID	= -1415637364122829574L;

	/**
	 * Constructs a {@code BackingStoreException} with the specified detail
	 * message.
	 * 
	 * @param message The detail message.
	 */
	public BackingStoreException(String message) {
		super(message);
	}

	/**
	 * Constructs a {@code BackingStoreException} with the specified detail
	 * message.
	 * 
	 * @param message The detail message.
	 * @param cause The cause of the exception. May be {@code null}.
	 * @since 1.1
	 */
	public BackingStoreException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Returns the cause of this exception or {@code null} if no cause was set.
	 * 
	 * @return The cause of this exception or {@code null} if no cause was set.
	 * @since 1.1
	 */
	@Override
	public Throwable getCause() {
		return super.getCause();
	}

	/**
	 * Initializes the cause of this exception to the specified value.
	 * 
	 * @param cause The cause of this exception.
	 * @return This exception.
	 * @throws IllegalArgumentException If the specified cause is this
	 *         exception.
	 * @throws IllegalStateException If the cause of this exception has already
	 *         been set.
	 * @since 1.1
	 */
	@Override
	public Throwable initCause(Throwable cause) {
		return super.initCause(cause);
	}
}
