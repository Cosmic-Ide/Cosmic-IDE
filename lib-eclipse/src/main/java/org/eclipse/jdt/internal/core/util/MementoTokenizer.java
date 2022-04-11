/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
package org.eclipse.jdt.internal.core.util;

import org.eclipse.jdt.internal.core.JavaElement;

public class MementoTokenizer {
	public static final String COUNT = Character.toString(JavaElement.JEM_COUNT);
	public static final String JAVAPROJECT = Character.toString(JavaElement.JEM_JAVAPROJECT);
	public static final String PACKAGEFRAGMENTROOT = Character.toString(JavaElement.JEM_PACKAGEFRAGMENTROOT);
	public static final String PACKAGEFRAGMENT = Character.toString(JavaElement.JEM_PACKAGEFRAGMENT);
	public static final String FIELD = Character.toString(JavaElement.JEM_FIELD);
	public static final String METHOD = Character.toString(JavaElement.JEM_METHOD);
	public static final String INITIALIZER = Character.toString(JavaElement.JEM_INITIALIZER);
	public static final String COMPILATIONUNIT = Character.toString(JavaElement.JEM_COMPILATIONUNIT);
	public static final String CLASSFILE = Character.toString(JavaElement.JEM_CLASSFILE);
	public static final String MODULAR_CLASSFILE = Character.toString(JavaElement.JEM_MODULAR_CLASSFILE);
	public static final String TYPE = Character.toString(JavaElement.JEM_TYPE);
	public static final String MODULE = Character.toString(JavaElement.JEM_MODULE);
	public static final String PACKAGEDECLARATION = Character.toString(JavaElement.JEM_PACKAGEDECLARATION);
	public static final String IMPORTDECLARATION = Character.toString(JavaElement.JEM_IMPORTDECLARATION);
	public static final String LOCALVARIABLE = Character.toString(JavaElement.JEM_LOCALVARIABLE);
	public static final String TYPE_PARAMETER = Character.toString(JavaElement.JEM_TYPE_PARAMETER);
	public static final String ANNOTATION = Character.toString(JavaElement.JEM_ANNOTATION);
	public static final String LAMBDA_EXPRESSION = Character.toString(JavaElement.JEM_LAMBDA_EXPRESSION);
	public static final String LAMBDA_METHOD = Character.toString(JavaElement.JEM_LAMBDA_METHOD);
	public static final String STRING = Character.toString(JavaElement.JEM_STRING);
	public static final String CLASSPATH_ATTRIBUTE = JAVAPROJECT+PACKAGEFRAGMENTROOT;

	private final char[] memento;
	private final int length;
	private int index = 0;

	public MementoTokenizer(String memento) {
		this.memento = memento.toCharArray();
		this.length = this.memento.length;
	}

	public boolean hasMoreTokens() {
		return this.index < this.length;
	}

	public String nextToken() {
		int start = this.index;
		StringBuilder buffer = null;
		switch (this.memento[this.index++]) {
			case JavaElement.JEM_ESCAPE:
				buffer = new StringBuilder();
				buffer.append(this.memento[this.index]);
				start = ++this.index;
				break;
			case JavaElement.JEM_COUNT:
				return COUNT;
			case JavaElement.JEM_JAVAPROJECT:
				// Also covers JavaElement#JEM_DELIMITER_ESCAPE, in which case,
				// we seek ahead by one char and check if it's an escaped delimiter
				// and if that's true, we return that as the token.
				// Else, we decide that JEM_JAVAPROJECT is the current token.
				if (this.index < this.length) {
					char nextChar = this.memento[this.index++];
					switch(nextChar) {
						case JavaElement.JEM_LAMBDA_EXPRESSION:
							return LAMBDA_EXPRESSION;
						case JavaElement.JEM_LAMBDA_METHOD:
							return LAMBDA_METHOD;
						case JavaElement.JEM_STRING:
							return STRING;
						case JavaElement.JEM_PACKAGEFRAGMENTROOT:
							return CLASSPATH_ATTRIBUTE;
						default:
							this.index--;
							break;
					}
				}
				return JAVAPROJECT;
			case JavaElement.JEM_PACKAGEFRAGMENTROOT:
				return PACKAGEFRAGMENTROOT;
			case JavaElement.JEM_PACKAGEFRAGMENT:
				return PACKAGEFRAGMENT;
			case JavaElement.JEM_FIELD:
				return FIELD;
			case JavaElement.JEM_METHOD:
				return METHOD;
			case JavaElement.JEM_INITIALIZER:
				return INITIALIZER;
			case JavaElement.JEM_COMPILATIONUNIT:
				return COMPILATIONUNIT;
			case JavaElement.JEM_CLASSFILE:
				return CLASSFILE;
			case JavaElement.JEM_MODULAR_CLASSFILE:
				return MODULAR_CLASSFILE;
			case JavaElement.JEM_TYPE:
				return TYPE;
			case JavaElement.JEM_MODULE:
				return MODULE;
			case JavaElement.JEM_PACKAGEDECLARATION:
				return PACKAGEDECLARATION;
			case JavaElement.JEM_IMPORTDECLARATION:
				return IMPORTDECLARATION;
			case JavaElement.JEM_LOCALVARIABLE:
				return LOCALVARIABLE;
			case JavaElement.JEM_TYPE_PARAMETER:
				return TYPE_PARAMETER;
			case JavaElement.JEM_ANNOTATION:
				return ANNOTATION;
		}
		loop: while (this.index < this.length) {
			switch (this.memento[this.index]) {
				case JavaElement.JEM_ESCAPE:
					if (buffer == null) buffer = new StringBuilder();
					buffer.append(this.memento, start, this.index - start);
					start = ++this.index;
					break;
				case JavaElement.JEM_COUNT:
				case JavaElement.JEM_JAVAPROJECT:
				case JavaElement.JEM_PACKAGEFRAGMENTROOT:
				case JavaElement.JEM_PACKAGEFRAGMENT:
				case JavaElement.JEM_FIELD:
				case JavaElement.JEM_METHOD:
				case JavaElement.JEM_INITIALIZER:
				case JavaElement.JEM_COMPILATIONUNIT:
				case JavaElement.JEM_CLASSFILE:
				case JavaElement.JEM_TYPE:
				case JavaElement.JEM_MODULE:
				case JavaElement.JEM_PACKAGEDECLARATION:
				case JavaElement.JEM_IMPORTDECLARATION:
				case JavaElement.JEM_LOCALVARIABLE:
				case JavaElement.JEM_TYPE_PARAMETER:
				case JavaElement.JEM_ANNOTATION:
					break loop;
			}
			this.index++;
		}
		if (buffer != null) {
			buffer.append(this.memento, start, this.index - start);
			return buffer.toString();
		} else {
			return new String(this.memento, start, this.index - start);
		}
	}

	public String getStringDelimitedBy(String delimiter) {
		String token = nextToken();
		if (token == delimiter)
			return ""; //$NON-NLS-1$
		String separator = nextToken();
		assert separator == delimiter;
		return token;
	}
}
