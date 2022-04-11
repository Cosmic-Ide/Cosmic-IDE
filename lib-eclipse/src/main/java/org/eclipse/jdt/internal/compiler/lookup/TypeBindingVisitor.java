/*******************************************************************************
 * Copyright (c) 2013, 2016 IBM Corporation and others.
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

import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.util.SimpleLookupTable;


public class TypeBindingVisitor {

	private SimpleLookupTable visitedCache;

	public void reset() {
		this.visitedCache = null;
	}

	public boolean visit(BaseTypeBinding baseTypeBinding)  {
		return true;  // continue traversal.
	}

	public boolean visit(ArrayBinding arrayBinding) {
		return true;  // continue traversal.
	}

	public boolean visit(TypeVariableBinding typeVariable) {
		return true;  // continue traversal.
	}

	public boolean visit(ReferenceBinding referenceBinding) {
		return true;  // continue traversal.
	}

	public boolean visit(WildcardBinding wildcardBinding) {
		return true;  // continue traversal.
	}

	public boolean visit(ParameterizedTypeBinding parameterizedTypeBinding) {
		return true;  // continue traversal.
	}

	public boolean visit(IntersectionTypeBinding18 intersectionTypeBinding18) {
		return true;  // continue traversal.
	}

	public boolean visit(RawTypeBinding rawTypeBinding) {
		return true;  // continue traversal.
	}

	public boolean visit(PolyTypeBinding polyTypeBinding) {
		return true;  // continue traversal.
	}

	public static void visit(TypeBindingVisitor visitor, ReferenceBinding[] types) {
		for (int i = 0, length = types == null ? 0 : types.length; i < length; i++) {
	        visit(visitor, types[i]);
	    }
	}

	public static void visit(TypeBindingVisitor visitor, TypeBinding type) {

		if (type == null)
			return;

		SimpleLookupTable visitedCache = visitor.visitedCache;
		if (visitedCache == null) {
			visitor.visitedCache = new SimpleLookupTable(3);
			visitedCache = visitor.visitedCache;
		}

		Object result = visitedCache.get(type);
		if (result == Boolean.TRUE)
			return;
		visitedCache.put(type, Boolean.TRUE);
		switch (type.kind()) {

			case Binding.TYPE_PARAMETER:
				TypeVariableBinding typeVariableBinding = (TypeVariableBinding) type;
				if (visitor.visit(typeVariableBinding)) {
					visit(visitor, typeVariableBinding.firstBound);
					visit(visitor, typeVariableBinding.superclass);
					visit(visitor, typeVariableBinding.superInterfaces);
				}
	            break;

			case Binding.PARAMETERIZED_TYPE:
				ParameterizedTypeBinding parameterizedTypeBinding = (ParameterizedTypeBinding) type;
				if (visitor.visit(parameterizedTypeBinding)) {
					visit(visitor, parameterizedTypeBinding.enclosingType());
					visit(visitor, parameterizedTypeBinding.arguments);
				}
				break;

			case Binding.ARRAY_TYPE:
				ArrayBinding arrayBinding = (ArrayBinding) type;
				if (visitor.visit(arrayBinding))
					visit(visitor, arrayBinding.leafComponentType);
				break;

			case Binding.WILDCARD_TYPE:
			case Binding.INTERSECTION_TYPE:
		        WildcardBinding wildcard = (WildcardBinding) type;
		        if (visitor.visit(wildcard)) {
		        	if (wildcard.boundKind != Wildcard.UNBOUND) {
		        		visit(visitor, wildcard.bound);
		        		visit(visitor, wildcard.otherBounds);
		        	}
		        }
				break;

			case Binding.BASE_TYPE:
				visitor.visit((BaseTypeBinding) type);
				break;

			case Binding.RAW_TYPE:
				visitor.visit((RawTypeBinding) type);
				break;

			case Binding.TYPE:
			case Binding.GENERIC_TYPE:
				ReferenceBinding referenceBinding = (ReferenceBinding) type;
				if (visitor.visit(referenceBinding)) {
					visit(visitor, referenceBinding.enclosingType());
					visit(visitor, referenceBinding.typeVariables());
				}
				break;

			case Binding.INTERSECTION_TYPE18:
				IntersectionTypeBinding18 intersectionTypeBinding18 = (IntersectionTypeBinding18) type;
				if (visitor.visit(intersectionTypeBinding18))
					visit(visitor, intersectionTypeBinding18.intersectingTypes);
				break;

			case Binding.POLY_TYPE:
				visitor.visit((PolyTypeBinding) type);
				break;

			default:
				throw new InternalError("Unexpected binding type"); //$NON-NLS-1$
		}
	}

	public static void visit(TypeBindingVisitor visitor, TypeBinding[] types) {
		for (int i = 0, length = types == null ? 0 : types.length; i < length; i++) {
	        visit(visitor, types[i]);
	    }
	}
}