/**********************************************************************
 * Copyright (c) 2008, 2014 Technical University Berlin, Germany and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *		Stephan Herrmann - Initial API and implementation
 **********************************************************************/
package org.eclipse.jdt.internal.compiler.util;

import java.util.Arrays;
import java.util.Comparator;

import org.eclipse.jdt.internal.compiler.lookup.InferenceVariable;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;

/**
 * Sorting utilities.
 * Originally developed for the <a href="http://www.eclipse.org/objectteams">Object Teams project</a>.
 */
public class Sorting {

	/**
	 * Topological sort for types
	 * Guarantee: supertypes come before subtypes.
	 */
	public static ReferenceBinding[] sortTypes(ReferenceBinding[] types) {
		int len = types.length;

		ReferenceBinding[] unsorted = new ReferenceBinding[len];
		ReferenceBinding[] sorted = new ReferenceBinding[len];
		System.arraycopy(types, 0, unsorted, 0, len);

		int o = 0;
		for(int i=0; i<len; i++)
			o = sort(unsorted, i, sorted, o);

		return sorted;
	}
	// Transfer input[i] and all its supers into output[o] ff.
	private static int sort(ReferenceBinding[] input, int i,
							ReferenceBinding[] output, int o)
	{
		if (input[i] == null)
			return o;

		ReferenceBinding superclass = input[i].superclass();
		o = sortSuper(superclass, input, output, o);

		ReferenceBinding[] superInterfaces = input[i].superInterfaces();
		for (int j=0; j<superInterfaces.length; j++) {
			o = sortSuper(superInterfaces[j], input, output, o);
		}

		// done with supers, now input[i] can safely be transferred:
		output[o++] = input[i];
		input[i] = null;

		return o;
	}
	// if superclass is within the set of types to sort,
	// transfer it and all its supers to output[o] ff.
	private static int sortSuper(ReferenceBinding superclass,
						  		 ReferenceBinding[] input,
						  		 ReferenceBinding[] output, int o)
	{
		if (superclass.id != TypeIds.T_JavaLangObject) {
			// search superclass within input:
			int j = 0;
			for(j=0; j<input.length; j++)
				if (TypeBinding.equalsEquals(input[j], superclass))
					break;
			if (j < input.length)
				// depth first traversal:
				o = sort(input, j, output, o);
			// otherwise assume super was already transferred.
		}
		return o;
	}
	public static MethodBinding[] concreteFirst(MethodBinding[] methods, int length) {
		if (length == 0 || (length > 0 && !methods[0].isAbstract()))
			return methods;
		MethodBinding[] copy = new MethodBinding[length];
		int idx = 0;
		for (int i=0; i<length; i++)
			if (!methods[i].isAbstract())
				copy[idx++] = methods[i];
		for (int i=0; i<length; i++)
			if (methods[i].isAbstract())
				copy[idx++] = methods[i];
		return copy;
	}
	public static MethodBinding[] abstractFirst(MethodBinding[] methods, int length) {
		if (length == 0 || (length > 0 && methods[0].isAbstract()))
			return methods;
		MethodBinding[] copy = new MethodBinding[length];
		int idx = 0;
		for (int i=0; i<length; i++)
			if (methods[i].isAbstract())
				copy[idx++] = methods[i];
		for (int i=0; i<length; i++)
			if (!methods[i].isAbstract())
				copy[idx++] = methods[i];
		return copy;
	}

	/** Sort inference variables by rank. */
	public static void sortInferenceVariables(InferenceVariable[] variables) {
		Arrays.sort(variables, new Comparator<InferenceVariable>() {
			@Override
			public int compare(InferenceVariable iv1, InferenceVariable iv2) {
				return iv1.rank - iv2.rank;
			}
		});
	}
}
