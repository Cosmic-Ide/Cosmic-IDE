/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.ast;

/**
 * Interface to allow Javadoc parser to collect both JavaSingleTypeReference and JavaQualifiedTypeReferences
 *
 * @author jjohnstn
 *
 */
public interface IJavadocTypeReference {

	public int getTagSourceStart();
	public int getTagSourceEnd();

}
