/*******************************************************************************
 * Copyright (c) 2015, 2016 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Stefan Xenos (Google) - Initial implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.classfmt;

import org.eclipse.jdt.internal.compiler.env.IBinaryAnnotation;
import org.eclipse.jdt.internal.compiler.env.IBinaryElementValuePair;
import org.eclipse.jdt.internal.compiler.env.IBinaryMethod;
import org.eclipse.jdt.internal.compiler.env.IBinaryTypeAnnotation;
import org.eclipse.jdt.internal.compiler.util.Util;

public class BinaryTypeFormatter {

	public static String annotationToString(IBinaryAnnotation annotation) {
		StringBuilder buffer = new StringBuilder();
		buffer.append('@');
		buffer.append(annotation.getTypeName());
		IBinaryElementValuePair[] valuePairs = annotation.getElementValuePairs();
		if (valuePairs != null) {
			buffer.append('(');
			buffer.append("\n\t"); //$NON-NLS-1$
			for (int i = 0, len = valuePairs.length; i < len; i++) {
				if (i > 0)
					buffer.append(",\n\t"); //$NON-NLS-1$
				buffer.append(valuePairs[i]);
			}
			buffer.append(')');
		}
		return buffer.toString();
	}

	public static String annotationToString(IBinaryTypeAnnotation typeAnnotation) {
		StringBuilder buffer = new StringBuilder();
		buffer.append(typeAnnotation.getAnnotation());
		buffer.append(' ');
		// Not fully decoding it here, just including all the information in the string
		buffer.append("target_type=").append(typeAnnotation.getTargetType()); //$NON-NLS-1$
		buffer.append(", info=").append(typeAnnotation.getSupertypeIndex()); //$NON-NLS-1$
		buffer.append(", info2=").append(typeAnnotation.getBoundIndex()); //$NON-NLS-1$
		int[] theTypePath = typeAnnotation.getTypePath();
		if (theTypePath != null && theTypePath.length != 0) {
			buffer.append(", location=["); //$NON-NLS-1$
			for (int i = 0, max = theTypePath.length; i < max; i += 2) {
				if (i > 0) {
					buffer.append(", "); //$NON-NLS-1$
				}
				switch (theTypePath[i]) {
					case 0:
						buffer.append("ARRAY"); //$NON-NLS-1$
						break;
					case 1:
						buffer.append("INNER_TYPE"); //$NON-NLS-1$
						break;
					case 2:
						buffer.append("WILDCARD"); //$NON-NLS-1$
						break;
					case 3:
						buffer.append("TYPE_ARGUMENT(").append(theTypePath[i+1]).append(')'); //$NON-NLS-1$
						break;
				}
			}
			buffer.append(']');
		}
		return buffer.toString();
	}

	public static String methodToString(IBinaryMethod method) {
		StringBuffer result = new StringBuffer();
		methodToStringContent(result, method);
		return result.toString();
	}

	public static void methodToStringContent(StringBuffer buffer, IBinaryMethod method) {
		int modifiers = method.getModifiers();
		char[] desc = method.getGenericSignature();
		if (desc == null)
			desc = method.getMethodDescriptor();
		buffer
			.append('{')
			.append(
				((modifiers & ClassFileConstants.AccDeprecated) != 0 ? "deprecated " : Util.EMPTY_STRING) //$NON-NLS-1$
					+ ((modifiers & 0x0001) == 1 ? "public " : Util.EMPTY_STRING) //$NON-NLS-1$
					+ ((modifiers & 0x0002) == 0x0002 ? "private " : Util.EMPTY_STRING) //$NON-NLS-1$
					+ ((modifiers & 0x0004) == 0x0004 ? "protected " : Util.EMPTY_STRING) //$NON-NLS-1$
					+ ((modifiers & 0x0008) == 0x000008 ? "static " : Util.EMPTY_STRING) //$NON-NLS-1$
					+ ((modifiers & 0x0010) == 0x0010 ? "final " : Util.EMPTY_STRING) //$NON-NLS-1$
					+ ((modifiers & 0x0040) == 0x0040 ? "bridge " : Util.EMPTY_STRING) //$NON-NLS-1$
					+ ((modifiers & 0x0080) == 0x0080 ? "varargs " : Util.EMPTY_STRING)) //$NON-NLS-1$
			.append(method.getSelector())
			.append(desc)
			.append('}');

		Object defaultValue = method.getDefaultValue();
		if (defaultValue != null) {
			buffer.append(" default "); //$NON-NLS-1$
			if (defaultValue instanceof Object[]) {
				buffer.append('{');
				Object[] elements = (Object[]) defaultValue;
				for (int i = 0, len = elements.length; i < len; i++) {
					if (i > 0)
						buffer.append(", "); //$NON-NLS-1$
					buffer.append(elements[i]);
				}
				buffer.append('}');
			} else {
				buffer.append(defaultValue);
			}
			buffer.append('\n');
		}

		IBinaryAnnotation[] annotations = method.getAnnotations();
		for (int i = 0, l = annotations == null ? 0 : annotations.length; i < l; i++) {
			buffer.append(annotations[i]);
			buffer.append('\n');
		}

		int annotatedParameterCount = method.getAnnotatedParametersCount();
		for (int i = 0; i < annotatedParameterCount; i++) {
			buffer.append("param" + (i - 1)); //$NON-NLS-1$
			buffer.append('\n');
			IBinaryAnnotation[] infos = method.getParameterAnnotations(i, new char[0]);
			for (int j = 0, k = infos == null ? 0 : infos.length; j < k; j++) {
				buffer.append(infos[j]);
				buffer.append('\n');
			}
		}
	}

}
