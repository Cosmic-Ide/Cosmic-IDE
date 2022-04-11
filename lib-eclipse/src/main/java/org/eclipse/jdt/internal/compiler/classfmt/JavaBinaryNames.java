package org.eclipse.jdt.internal.compiler.classfmt;

public class JavaBinaryNames {
	/**
	 * Returns true iff the given method selector is clinit.
	 */
	public static boolean isClinit(char[] selector) {
		return selector[0] == '<' && selector.length == 8; // Can only match <clinit>
	}

	/**
	 * Returns true iff the given method selector is a constructor.
	 */
	public static boolean isConstructor(char[] selector) {
		return selector[0] == '<' && selector.length == 6; // Can only match <init>
	}
}
