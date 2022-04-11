/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

public class UnresolvedAnnotationBinding extends AnnotationBinding {
	private LookupEnvironment env;
	private boolean typeUnresolved = true;

UnresolvedAnnotationBinding(ReferenceBinding type, ElementValuePair[] pairs, LookupEnvironment env) {
	super(type, pairs);
	this.env = env;
}

@Override
public void resolve() { // in place resolution.
	if (this.typeUnresolved) { // the type is resolved when requested
		boolean wasToleratingMissingTypeProcessingAnnotations = this.env.mayTolerateMissingType;
		this.env.mayTolerateMissingType = true; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=388042
		try {
			this.type = (ReferenceBinding) BinaryTypeBinding.resolveType(this.type, this.env, false /* no raw conversion for now */);
				// annotation types are never parameterized
		} finally {
			this.env.mayTolerateMissingType = wasToleratingMissingTypeProcessingAnnotations;
		}
		this.typeUnresolved = false;
	}
}
@Override
public ReferenceBinding getAnnotationType() {
	resolve();
	return this.type;
}

@Override
public ElementValuePair[] getElementValuePairs() {
	if (this.env != null) {
		if (this.typeUnresolved) {
			resolve();
		}
		// resolve method binding and value type (if unresolved) for each pair
		for (int i = this.pairs.length; --i >= 0;) {
			ElementValuePair pair = this.pairs[i];
			MethodBinding[] methods = this.type.getMethods(pair.getName());
			// there should be exactly one since the type is an annotation type.
			if (methods != null && methods.length == 1) {
				pair.setMethodBinding(methods[0]);
			} // else silently leave a null there
			Object value = pair.getValue();
			boolean wasToleratingMissingTypeProcessingAnnotations = this.env.mayTolerateMissingType;
			this.env.mayTolerateMissingType = true; // https://bugs.eclipse.org/bugs/show_bug.cgi?id=360164
			try {
				if (value instanceof UnresolvedReferenceBinding) {
					pair.setValue(((UnresolvedReferenceBinding) value).
							resolve(this.env, false));
								// no parameterized types in annotation values
				} else if (value instanceof Object[]) {
					Object[] values = (Object[]) value;
					for (int j = 0; j < values.length; j++) {
						if (values[j] instanceof UnresolvedReferenceBinding) {
							values[j] = ((UnresolvedReferenceBinding) values[j]).resolve(this.env, false);
						}
					}
				} // do nothing for UnresolvedAnnotationBinding-s, since their
				  // content is only accessed through get* methods
			} finally {
				this.env.mayTolerateMissingType = wasToleratingMissingTypeProcessingAnnotations;
			}
		}
		this.env = null;
	}
	return this.pairs;
}
}
