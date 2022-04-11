/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Stephan Herrmann <stephan@cs.tu-berlin.de> - Contributions for
 *     							bug 185682 - Increment/decrement operators mark local variables as read
 *     							bug 349326 - [1.7] new warning for missing try-with-resources
 *								bug 186342 - [compiler][null] Using annotations for null checking
 *								bug 365859 - [compiler][null] distinguish warnings based on flow analysis vs. null annotations
 *								bug 331649 - [compiler][null] consider null annotations for fields
 *								Bug 466308 - [hovering] Javadoc header for parameter is wrong with annotation-based null analysis
 *     Jesper S Møller <jesper@selskabet.org> -  Contributions for
 *								Bug 527554 - [18.3] Compiler support for JEP 286 Local-Variable Type
 *
 *******************************************************************************/
package org.eclipse.jdt.internal.compiler.lookup;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.ASTNode;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.FakedTrackingVariable;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.LambdaExpression;
import org.eclipse.jdt.internal.compiler.ast.LocalDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.impl.ReferenceContext;

public class LocalVariableBinding extends VariableBinding {

	public int resolvedPosition; // for code generation (position in method context)

	public static final int UNUSED = 0;
	public static final int USED = 1;
	public static final int FAKE_USED = 2;
	public int useFlag; // for flow analysis (default is UNUSED), values < 0 indicate the number of compound uses (postIncrement or compoundAssignment)

	public BlockScope declaringScope; // back-pointer to its declaring scope
	public LocalDeclaration declaration; // for source-positions

	public int[] initializationPCs;
	public int initializationCount = 0;

	public FakedTrackingVariable closeTracker; // track closing of instances of type AutoCloseable, maybe null

	public Set<MethodScope> uninitializedInMethod;

	// for synthetic local variables
	// if declaration slot is not positionned, the variable will not be listed in attribute
	// note that the name of a variable should be chosen so as not to conflict with user ones (usually starting with a space char is all needed)
	public LocalVariableBinding(char[] name, TypeBinding type, int modifiers, boolean isArgument) {
		super(name, type, modifiers, isArgument ? Constant.NotAConstant : null);
		if (isArgument) this.tagBits |= TagBits.IsArgument;
		this.tagBits |= TagBits.IsEffectivelyFinal;
	}

	// regular local variable or argument
	public LocalVariableBinding(LocalDeclaration declaration, TypeBinding type, int modifiers, boolean isArgument) {

		this(declaration.name, type, modifiers, isArgument);
		this.declaration = declaration;
	}

	// argument
	public LocalVariableBinding(LocalDeclaration declaration, TypeBinding type, int modifiers, MethodScope declaringScope) {

		this(declaration, type, modifiers, true);
		this.declaringScope = declaringScope;
	}

	/* API
	* Answer the receiver's binding type from Binding.BindingID.
	*/
	@Override
	public final int kind() {
		return LOCAL;
	}

	/*
	 * declaringUniqueKey # scopeIndex(0-based) # varName [# occurrenceCount(0-based)]
	 *    p.X { void foo() { int local; int local;} } --> Lp/X;.foo()V#1#local#1
	 *
	 * for method parameter, we have no scopeIndex, but instead we append the parameter rank:
	 * declaringUniqueKey # varName # occurrenceCount(always 0) # argument rank (0-based)
	 * with parameter names:
	 *    p.X { void foo(int i0, int i1) { } } --> Lp/X;.foo()V#i1#0#1
	 * without parameter names (see org.eclipse.jdt.internal.core.util.BindingKeyResolver.SyntheticLocalVariableBinding):
	 *    p.X { void foo(int i0, int i1) { } } --> Lp/X;.foo()V#arg1#0#1
	 */
	@Override
	public char[] computeUniqueKey(boolean isLeaf) {
		StringBuffer buffer = new StringBuffer();

		// declaring method or type
		BlockScope scope = this.declaringScope;
		int occurenceCount = 0;
		if (scope != null) {
			// the scope can be null. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=185129
			MethodScope methodScope = scope instanceof MethodScope ? (MethodScope) scope : scope.enclosingMethodScope();
			ReferenceContext referenceContext = methodScope.referenceContext;
			if (referenceContext instanceof AbstractMethodDeclaration) {
				MethodBinding methodBinding = ((AbstractMethodDeclaration) referenceContext).binding;
				if (methodBinding != null) {
					buffer.append(methodBinding.computeUniqueKey(false/*not a leaf*/));
				}
			} else if (referenceContext instanceof TypeDeclaration) {
				TypeBinding typeBinding = ((TypeDeclaration) referenceContext).binding;
				if (typeBinding != null) {
					buffer.append(typeBinding.computeUniqueKey(false/*not a leaf*/));
				}
			} else if (referenceContext instanceof LambdaExpression) {
				MethodBinding methodBinding = ((LambdaExpression) referenceContext).binding;
				if (methodBinding != null) {
					buffer.append(methodBinding.computeUniqueKey(false/*not a leaf*/));
				}
			}

			// scope index
			getScopeKey(scope, buffer);

			// find number of occurences of a variable with the same name in the scope
			LocalVariableBinding[] locals = scope.locals;
			for (int i = 0; i < scope.localIndex; i++) { // use linear search assuming the number of locals per scope is low
				LocalVariableBinding local = locals[i];
				if (CharOperation.equals(this.name, local.name)) {
					if (this == local)
						break;
					occurenceCount++;
				}
			}
		}
		// variable name
		buffer.append('#');
		buffer.append(this.name);

		boolean addParameterRank = this.isParameter() && this.declaringScope != null;
		// add occurence count to avoid same key for duplicate variables
		// (see https://bugs.eclipse.org/bugs/show_bug.cgi?id=149590)
		if (occurenceCount > 0 || addParameterRank) {
			buffer.append('#');
			buffer.append(occurenceCount);
			if (addParameterRank) {
				int pos = -1;
				LocalVariableBinding[] params = this.declaringScope.locals;
				for (int i = 0; i < params.length; i++) {
					if (params[i] == this) {
						pos = i;
						break;
					}
				}
				if (pos > -1) {
					buffer.append('#');
					buffer.append(pos);
				}
			}
		}

		int length = buffer.length();
		char[] uniqueKey = new char[length];
		buffer.getChars(0, length, uniqueKey, 0);
		return uniqueKey;
	}

	@Override
	public AnnotationBinding[] getAnnotations() {
		if (this.declaringScope == null) {
			if ((this.tagBits & TagBits.AnnotationResolved) != 0) {
				// annotation are already resolved
				if (this.declaration == null) {
					return Binding.NO_ANNOTATIONS;
				}
				Annotation[] annotations = this.declaration.annotations;
				if (annotations != null) {
					int length = annotations.length;
					AnnotationBinding[] annotationBindings = new AnnotationBinding[length];
					for (int i = 0; i < length; i++) {
						AnnotationBinding compilerAnnotation = annotations[i].getCompilerAnnotation();
						if (compilerAnnotation == null) {
							return Binding.NO_ANNOTATIONS;
						}
						annotationBindings[i] = compilerAnnotation;
					}
					return annotationBindings;
				}
			}
			return Binding.NO_ANNOTATIONS;
		}
		SourceTypeBinding sourceType = this.declaringScope.enclosingSourceType();
		if (sourceType == null)
			return Binding.NO_ANNOTATIONS;

		if ((this.tagBits & TagBits.AnnotationResolved) == 0) {
			if (((this.tagBits & TagBits.IsArgument) != 0) && this.declaration != null) {
				Annotation[] annotationNodes = this.declaration.annotations;
				if (annotationNodes != null) {
					ASTNode.resolveAnnotations(this.declaringScope, annotationNodes, this, true);
				}
			}
		}
		return sourceType.retrieveAnnotations(this);
	}

	private void getScopeKey(BlockScope scope, StringBuffer buffer) {
		int scopeIndex = scope.scopeIndex();
		if (scopeIndex != -1) {
			getScopeKey((BlockScope)scope.parent, buffer);
			buffer.append('#');
			buffer.append(scopeIndex);
		}
	}

	// Answer whether the variable binding is a secret variable added for code gen purposes
	public boolean isSecret() {

		return this.declaration == null && (this.tagBits & TagBits.IsArgument) == 0;
	}

	public void recordInitializationEndPC(int pc) {

		if (this.initializationPCs[((this.initializationCount - 1) << 1) + 1] == -1)
			this.initializationPCs[((this.initializationCount - 1) << 1) + 1] = pc;
	}

	public void recordInitializationStartPC(int pc) {

		if (this.initializationPCs == null) {
			return;
		}
		if (this.initializationCount > 0) {
			int previousEndPC = this.initializationPCs[ ((this.initializationCount - 1) << 1) + 1];
			 // interval still open, keep using it (108180)
			if (previousEndPC == -1) {
				return;
			}
			// optimize cases where reopening a contiguous interval
			if (previousEndPC == pc) {
				this.initializationPCs[ ((this.initializationCount - 1) << 1) + 1] = -1; // reuse previous interval (its range will be augmented)
				return;
			}
		}
		int index = this.initializationCount << 1;
		if (index == this.initializationPCs.length) {
			System.arraycopy(this.initializationPCs, 0, (this.initializationPCs = new int[this.initializationCount << 2]), 0, index);
		}
		this.initializationPCs[index] = pc;
		this.initializationPCs[index + 1] = -1;
		this.initializationCount++;
	}

	@Override
	public void setAnnotations(AnnotationBinding[] annotations, Scope scope, boolean forceStore) {
		// note: we don's use this.declaringScope because we might be called before Scope.addLocalVariable(this)
		//       which is where this.declaringScope is set.
		if (scope == null)
			return;
		SourceTypeBinding sourceType = scope.enclosingSourceType();
		if (sourceType != null)
			sourceType.storeAnnotations(this, annotations, forceStore);
	}

	public void resetInitializations() {
		this.initializationCount = 0;
		this.initializationPCs = null;
	}

	@Override
	public String toString() {

		String s = super.toString();
		switch (this.useFlag){
			case USED:
				s += "[pos: " + String.valueOf(this.resolvedPosition) + "]"; //$NON-NLS-2$ //$NON-NLS-1$
				break;
			case UNUSED:
				s += "[pos: unused]"; //$NON-NLS-1$
				break;
			case FAKE_USED:
				s += "[pos: fake_used]"; //$NON-NLS-1$
				break;
		}
		s += "[id:" + String.valueOf(this.id) + "]"; //$NON-NLS-2$ //$NON-NLS-1$
		if (this.initializationCount > 0) {
			s += "[pc: "; //$NON-NLS-1$
			for (int i = 0; i < this.initializationCount; i++) {
				if (i > 0)
					s += ", "; //$NON-NLS-1$
				s += String.valueOf(this.initializationPCs[i << 1]) + "-" + ((this.initializationPCs[(i << 1) + 1] == -1) ? "?" : String.valueOf(this.initializationPCs[(i<< 1) + 1])); //$NON-NLS-2$ //$NON-NLS-1$
			}
			s += "]"; //$NON-NLS-1$
		}
		return s;
	}

	@Override
	public boolean isParameter() {
		return ((this.tagBits & TagBits.IsArgument) != 0);
	}

	public boolean isCatchParameter() {
		return false;
	}
	public boolean isPatternVariable() {
		return ((this.modifiers & ExtraCompilerModifiers.AccPatternVariable) != 0);
	}

	public MethodBinding getEnclosingMethod() {
		BlockScope blockScope = this.declaringScope;
		if (blockScope != null) {
			ReferenceContext referenceContext = blockScope.referenceContext();
			if (referenceContext instanceof Initializer) {
				return null;
			}
			if (referenceContext instanceof AbstractMethodDeclaration) {
				return ((AbstractMethodDeclaration) referenceContext).binding;
			}
		}
		return null;
	}

	public void markInitialized() {
		// Signals that the type is correctly set now - This is for extension in subclasses
	}
	public void markReferenced() {
		// Signal that the name is used - This is for extension in subclasses
	}

	public boolean isUninitializedIn(Scope scope) {
		if (this.uninitializedInMethod != null)
			return this.uninitializedInMethod.contains(scope.methodScope());
		return false;
	}

	public void markAsUninitializedIn(Scope scope) {
		if (this.uninitializedInMethod == null)
			this.uninitializedInMethod = new HashSet<>();
		this.uninitializedInMethod.add(scope.methodScope());
	}
}
