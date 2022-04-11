/*******************************************************************************
 * Copyright (c) 2020, 2021 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

public interface ExtendedTagBits {

	int AreRecordComponentsComplete = ASTNode.Bit1;
	int HasUnresolvedPermittedSubtypes = ASTNode.Bit2;

	/** From Java 16
	 *  Flag used to identify the annotation jdk.internal.ValueBased
	 */
	int AnnotationValueBased = ASTNode.Bit3;

	// Java 16 Records
	int IsCanonicalConstructor = ASTNode.Bit4; // constructor
	int isImplicit  = ASTNode.Bit5; // constructor and method

}
