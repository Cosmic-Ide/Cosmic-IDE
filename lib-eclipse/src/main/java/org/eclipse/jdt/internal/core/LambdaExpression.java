/*******************************************************************************
 * Copyright (c) 2014, 2015 IBM Corporation and others.
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
 *     Stephan Herrmann - Contribution for
 *							Bug 458613 - [1.8] lambda not shown in quick type hierarchy
 *******************************************************************************/
package org.eclipse.jdt.internal.core;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.ILocalVariable;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.ITypeRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.lookup.Binding;
import org.eclipse.jdt.internal.compiler.lookup.LookupEnvironment;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.Scope;
import org.eclipse.jdt.internal.compiler.lookup.Substitution;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.core.util.MementoTokenizer;
import org.eclipse.jdt.internal.core.util.Util;

public class LambdaExpression extends SourceType {

	SourceTypeElementInfo elementInfo;
	LambdaMethod lambdaMethod;

	// These fields could be materialized from elementInfo, but for ease of use stashed here
	protected int sourceStart;
	protected int sourceEnd;
	protected int arrowPosition;
	protected String interphase;


	// Construction from AST node
	LambdaExpression(JavaElement parent, org.eclipse.jdt.internal.compiler.ast.LambdaExpression lambdaExpression) {
		super(parent, new String(CharOperation.NO_CHAR));
		this.sourceStart = lambdaExpression.sourceStart;
		this.sourceEnd = lambdaExpression.sourceEnd;
		this.arrowPosition = lambdaExpression.arrowPosition;

		TypeBinding supertype = findLambdaSuperType(lambdaExpression);
		this.interphase = new String(CharOperation.replaceOnCopy(supertype.genericTypeSignature(), '/', '.'));
		this.elementInfo = makeTypeElementInfo(this, this.interphase, this.sourceStart, this.sourceEnd, this.arrowPosition);
		this.lambdaMethod = LambdaFactory.createLambdaMethod(this, lambdaExpression);
		this.elementInfo.children = new IJavaElement[] { this.lambdaMethod };
	}

	public TypeBinding findLambdaSuperType(org.eclipse.jdt.internal.compiler.ast.LambdaExpression lambdaExpression) {
		// start from the specific type, ignoring type arguments:
		TypeBinding original = lambdaExpression.resolvedType.original();
		// infer type arguments from here:
		final TypeBinding descType = lambdaExpression.descriptor.declaringClass;
		if (descType instanceof ParameterizedTypeBinding) {
			final ParameterizedTypeBinding descPTB = (ParameterizedTypeBinding) descType;
			// intermediate type: original pulled up to the level of descType:
			final TypeBinding originalSuper = original.findSuperTypeOriginatingFrom(descType);
			return Scope.substitute(new Substitution() {
							@Override
							public TypeBinding substitute(TypeVariableBinding typeVariable) {
								if (originalSuper instanceof ParameterizedTypeBinding) {
									ParameterizedTypeBinding originalSuperPTB = (ParameterizedTypeBinding) originalSuper;
									TypeBinding[] superArguments = originalSuperPTB.arguments;
									for (int i = 0; i < superArguments.length; i++) {
										// if originalSuper holds typeVariable as it i'th argument, then the i'th argument of descType is our answer:
										if (TypeBinding.equalsEquals(superArguments[i], typeVariable))
											return descPTB.arguments[i];
									}
								}
								// regular substitution:
								return descPTB.substitute(typeVariable);
							}
							@Override
							public boolean isRawSubstitution() {
								return descPTB.isRawType();
							}
							@Override
							public LookupEnvironment environment() {
								return descPTB.environment;
							}
						}, original);
		}
		return original;
	}

	// Construction from memento
	LambdaExpression(JavaElement parent, String interphase, int sourceStart, int sourceEnd, int arrowPosition) {
		super(parent, new String(CharOperation.NO_CHAR));
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
		this.arrowPosition = arrowPosition;
		this.interphase = interphase;
		this.elementInfo = makeTypeElementInfo(this, interphase, this.sourceStart = sourceStart, sourceEnd, arrowPosition);
		// Method is in the process of being fabricated, will be attached shortly.
	}

	// Construction from subtypes.
	LambdaExpression(JavaElement parent, String interphase, int sourceStart, int sourceEnd, int arrowPosition, LambdaMethod lambdaMethod) {
		super(parent, new String(CharOperation.NO_CHAR));
		this.sourceStart = sourceStart;
		this.sourceEnd = sourceEnd;
		this.arrowPosition = arrowPosition;
		this.interphase = interphase;
		this.elementInfo = makeTypeElementInfo(this, interphase, this.sourceStart = sourceStart, sourceEnd, arrowPosition);
		this.elementInfo.children = new IJavaElement[] { this.lambdaMethod = lambdaMethod };
	}

	// Lambda expression is not backed by model, fabricate element information structure and stash it.
	static private SourceTypeElementInfo makeTypeElementInfo (LambdaExpression handle, String interphase, int sourceStart, int sourceEnd, int arrowPosition) {

		SourceTypeElementInfo elementInfo = new SourceTypeElementInfo();

		elementInfo.setFlags(0);
		elementInfo.setHandle(handle);
		elementInfo.setSourceRangeStart(sourceStart);
		elementInfo.setSourceRangeEnd(sourceEnd);

		elementInfo.setNameSourceStart(sourceStart);
		elementInfo.setNameSourceEnd(arrowPosition);
		elementInfo.setSuperclassName(null);
		elementInfo.addCategories(handle, null);

		JavaModelManager manager = JavaModelManager.getJavaModelManager();
		char[][] superinterfaces = new char [][] { manager.intern(Signature.toString(interphase).toCharArray()) }; // drops marker interfaces - to fix.
		elementInfo.setSuperInterfaceNames(superinterfaces);
		return elementInfo;
	}

	@Override
	protected void closing(Object info) throws JavaModelException {
		// nothing to do, not backed by model ATM.
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		/* I see cases where equal lambdas are dismissed as unequal on account of working copy owner.
		   This results in spurious failures. See JavaSearchBugs8Tests.testBug400905_0021()
		   For now exclude the working copy owner and compare
		*/
		if (o instanceof LambdaExpression) {
			LambdaExpression that = (LambdaExpression) o;
			if (this.sourceStart != that.sourceStart)
				return false;
			ITypeRoot thisTR = this.getTypeRoot();
			ITypeRoot thatTR = that.getTypeRoot();
			return thisTR.getElementName().equals(thatTR.getElementName()) && thisTR.getParent().equals(thatTR.getParent());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Util.combineHashCodes(super.hashCode(), this.sourceStart);
	}

	@Override
	public Object getElementInfo(IProgressMonitor monitor) throws JavaModelException {
		return this.elementInfo;
	}

	@Override
	protected char getHandleMementoDelimiter() {
		return JavaElement.JEM_LAMBDA_EXPRESSION;
	}

	@Override
	protected void getHandleMemento(StringBuffer buff) {
		getHandleMemento(buff, true, true);
		// lambda method and lambda expression cannot share the same memento - add a trailing discriminator.
		appendEscapedDelimiter(buff, getHandleMementoDelimiter());
	}

	protected void getHandleMemento(StringBuffer buff, boolean serializeParent, boolean serializeChild) {
		if (serializeParent)
			getParent().getHandleMemento(buff);
		appendEscapedDelimiter(buff, getHandleMementoDelimiter());
		appendEscapedDelimiter(buff, JEM_STRING);
		escapeMementoName(buff, this.interphase);
		buff.append(JEM_COUNT);
		buff.append(this.sourceStart);
		buff.append(JEM_COUNT);
		buff.append(this.sourceEnd);
		buff.append(JEM_COUNT);
		buff.append(this.arrowPosition);
		if (serializeChild)
			this.lambdaMethod.getHandleMemento(buff, false);
	}

	@Override
	public IJavaElement getHandleFromMemento(String token, MementoTokenizer memento, WorkingCopyOwner workingCopyOwner) {

		if (token.charAt(0) != JEM_LAMBDA_METHOD)
			return null;

		// ----
		if (!memento.hasMoreTokens()) return this;
		String selector = memento.nextToken();
		if (!memento.hasMoreTokens() || memento.nextToken().charAt(0) != JEM_COUNT) return this;
		if (!memento.hasMoreTokens()) return this;
		int length = Integer.parseInt(memento.nextToken());
		String [] parameterTypes = new String[length];
		String [] parameterNames = new String[length];
		for (int i = 0; i < length; i++) {
			if (!memento.hasMoreTokens() || memento.nextToken().charAt(0) != JEM_STRING) return this;
			parameterTypes[i] = memento.nextToken();
			if (!memento.hasMoreTokens() || memento.nextToken().charAt(0) != JEM_STRING) return this;
			parameterNames[i] = memento.nextToken();
		}
		if (!memento.hasMoreTokens() || memento.nextToken().charAt(0) != JEM_STRING) return this;
		String returnType = memento.nextToken();
		if (!memento.hasMoreTokens() || memento.nextToken().charAt(0) != JEM_STRING) return this;
		String key = memento.nextToken();
		this.lambdaMethod = LambdaFactory.createLambdaMethod(this, selector, key, this.sourceStart, this.sourceEnd, this.arrowPosition, parameterTypes, parameterNames, returnType);
		ILocalVariable [] parameters = new ILocalVariable[length];
		for (int i = 0; i < length; i++) {
			parameters[i] = (ILocalVariable) this.lambdaMethod.getHandleFromMemento(memento, workingCopyOwner);
		}
		this.lambdaMethod.elementInfo.arguments  = parameters;
		this.elementInfo.children = new IJavaElement[] { this.lambdaMethod };
		if (!memento.hasMoreTokens())
			return this.lambdaMethod;
		switch (memento.nextToken().charAt(0)) {
			case JEM_LAMBDA_METHOD:
				if (!memento.hasMoreTokens())
					return this.lambdaMethod;
				return this.lambdaMethod.getHandleFromMemento(memento, workingCopyOwner);
			case JEM_LAMBDA_EXPRESSION:
			default:
				return this;
		}
	}

	@Override
	public IJavaElement[] getChildren() throws JavaModelException {
		return new IJavaElement[] { this.lambdaMethod };
	}

	@Override
	public boolean isLocal() {
		return true;
	}

	@Override
	public JavaElement resolved(Binding binding) {
		ResolvedLambdaExpression resolvedHandle = new ResolvedLambdaExpression(this.getParent(), this, new String(binding.computeUniqueKey()));
		return resolvedHandle;
	}

	public IMethod getMethod() {
		return this.lambdaMethod;
	}

	@Override
	public boolean isLambda() {
		return true;
	}

	@Override
	public boolean isAnonymous() {
		return false;
	}

	@Override
	public void toStringName(StringBuffer buffer) {
		super.toStringName(buffer);
		buffer.append("<lambda #"); //$NON-NLS-1$
		buffer.append(this.occurrenceCount);
		buffer.append(">"); //$NON-NLS-1$
	}

	@Override
	public JavaElement getPrimaryElement(boolean checkOwner) {
		if (checkOwner) {
			CompilationUnit cu = (CompilationUnit)getAncestor(COMPILATION_UNIT);
			if (cu == null || cu.isPrimary()) return this;
		}
		IJavaElement primaryParent = this.getParent().getPrimaryElement(false);
		if (primaryParent instanceof JavaElement) {
			JavaElement ancestor = (JavaElement) primaryParent;
			StringBuffer buffer = new StringBuffer(32);
			getHandleMemento(buffer, false, true);
			String memento = buffer.toString();
			return (JavaElement) ancestor.getHandleFromMemento(new MementoTokenizer(memento), DefaultWorkingCopyOwner.PRIMARY).getParent();
		}
		return this;
	}

	@Override
	public String[] getSuperInterfaceTypeSignatures() throws JavaModelException {
		return new String[] { this.interphase };
	}
}
