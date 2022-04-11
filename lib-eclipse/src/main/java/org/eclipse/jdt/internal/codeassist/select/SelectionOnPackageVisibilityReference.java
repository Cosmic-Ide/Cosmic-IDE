/*******************************************************************************
 * Copyright (c) 2017 IBM Corporation and others.
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
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.codeassist.select;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ImportReference;

/*
 * Selection node build by the parser in any case it was intending to
 * reduce an export reference containing the assist identifier.
 * e.g.
 *
 *	module myModule {
 *  exports packageo[cursor];
 *  }
 *
 *	module myModule {
 *	---> <SelectionOnExport:packageo>
 *  }
 *
 */
public class SelectionOnPackageVisibilityReference extends ImportReference {

	public SelectionOnPackageVisibilityReference(char[][] tokens, long[] positions) {
		super(tokens, positions, false, 0);
	}

	@Override
	public StringBuffer print(int indent, StringBuffer output) {

		printIndent(indent, output).append("<SelectOnPackageVisibility:"); //$NON-NLS-1$
		output.append(new String(CharOperation.concatWith(this.tokens, '.')));
		return output.append('>');
	}
}
