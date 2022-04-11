/*******************************************************************************
 * Copyright (c) 2018 Till Brychcy and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Till Brychcy - initial API and implementation
 *******************************************************************************/

package org.eclipse.jdt.internal.compiler.lookup;

interface ParameterNonNullDefaultProvider {
	public boolean hasAnyNonNullDefault();
	public boolean hasNonNullDefaultForParam(int i);

	public static final ParameterNonNullDefaultProvider FALSE_PROVIDER = new ParameterNonNullDefaultProvider() {
		@Override
		public boolean hasNonNullDefaultForParam(int i) {
			return false;
		}

		@Override
		public boolean hasAnyNonNullDefault() {
			return false;
		}
	};
	public static final ParameterNonNullDefaultProvider TRUE_PROVIDER = new ParameterNonNullDefaultProvider() {
		@Override
		public boolean hasNonNullDefaultForParam(int i) {
			return true;
		}

		@Override
		public boolean hasAnyNonNullDefault() {
			return true;
		}
	};
	public static class MixedProvider implements ParameterNonNullDefaultProvider {
		private final boolean[] result;

		public MixedProvider(boolean[] result) {
			this.result = result;
		}

		@Override
		public boolean hasNonNullDefaultForParam(int i) {
			return this.result[i];
		}

		@Override
		public boolean hasAnyNonNullDefault() {
			return true;
		}
	}
}